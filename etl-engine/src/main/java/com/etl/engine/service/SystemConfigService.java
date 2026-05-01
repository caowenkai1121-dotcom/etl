package com.etl.engine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.etl.common.config.ConfigChangeEvent;
import com.etl.common.config.ConfigClient;
import com.etl.engine.entity.EtlSystemConfig;
import com.etl.engine.mapper.SystemConfigMapper;
import com.etl.common.result.PageResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 系统配置服务
 */
@Slf4j
@Service
public class SystemConfigService implements ConfigClient {

    @Autowired
    private SystemConfigMapper systemConfigMapper;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    private final ConcurrentHashMap<String, EtlSystemConfig> configCache = new ConcurrentHashMap<>();

    /**
     * 系统启动时加载所有配置到缓存
     */
    @PostConstruct
    public void init() {
        refresh();
        log.info("系统配置加载完成，共加载 {} 条配置", configCache.size());
    }

    /**
     * 刷新配置缓存
     */
    @Override
    public void refresh() {
        List<EtlSystemConfig> configs = systemConfigMapper.selectList(null);
        configCache.clear();
        for (EtlSystemConfig config : configs) {
            configCache.put(buildKey(config.getConfigGroup(), config.getConfigKey()), config);
        }
        log.info("配置缓存刷新完成，共 {} 条配置", configCache.size());
    }

    /**
     * 分页查询配置列表
     */
    public PageResult<EtlSystemConfig> pageList(int page, int size, String group) {
        Page<EtlSystemConfig> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<EtlSystemConfig> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(group)) {
            queryWrapper.eq(EtlSystemConfig::getConfigGroup, group);
        }
        Page<EtlSystemConfig> result = systemConfigMapper.selectPage(pageParam, queryWrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
    }

    /**
     * 获取字符串类型配置
     */
    @Override
    public String getString(String group, String key, String defaultValue) {
        EtlSystemConfig config = configCache.get(buildKey(group, key));
        return config != null ? config.getConfigValue() : defaultValue;
    }

    /**
     * 获取整数类型配置
     */
    @Override
    public int getInt(String group, String key, int defaultValue) {
        String value = getString(group, key, null);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                log.warn("配置值 {} 不是有效的整数，使用默认值 {}", value, defaultValue);
            }
        }
        return defaultValue;
    }

    /**
     * 获取长整数类型配置
     */
    @Override
    public long getLong(String group, String key, long defaultValue) {
        String value = getString(group, key, null);
        if (value != null) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException e) {
                log.warn("配置值 {} 不是有效的长整数，使用默认值 {}", value, defaultValue);
            }
        }
        return defaultValue;
    }

    /**
     * 获取双精度浮点数类型配置
     */
    @Override
    public double getDouble(String group, String key, double defaultValue) {
        String value = getString(group, key, null);
        if (value != null) {
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException e) {
                log.warn("配置值 {} 不是有效的浮点数，使用默认值 {}", value, defaultValue);
            }
        }
        return defaultValue;
    }

    /**
     * 获取布尔类型配置
     */
    @Override
    public boolean getBoolean(String group, String key, boolean defaultValue) {
        String value = getString(group, key, null);
        if (value != null) {
            return Boolean.parseBoolean(value);
        }
        return defaultValue;
    }

    /**
     * 更新配置
     */
    public void updateConfig(String group, String key, String value, String description) {
        // 验证配置类型
        EtlSystemConfig existingConfig = configCache.get(buildKey(group, key));
        if (existingConfig != null) {
            validateConfigType(existingConfig.getConfigType(), value);
        }

        LambdaQueryWrapper<EtlSystemConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(EtlSystemConfig::getConfigGroup, group)
                   .eq(EtlSystemConfig::getConfigKey, key);

        EtlSystemConfig config = systemConfigMapper.selectOne(queryWrapper);
        if (config != null) {
            config.setConfigValue(value);
            if (StringUtils.hasText(description)) {
                config.setDescription(description);
            }
            systemConfigMapper.updateById(config);
        } else {
            // 新建配置
            config = new EtlSystemConfig();
            config.setConfigGroup(group);
            config.setConfigKey(key);
            config.setConfigValue(value);
            config.setDescription(StringUtils.hasText(description) ? description : "");
            config.setConfigType(detectConfigType(value));
            config.setIsEditable(1);
            systemConfigMapper.insert(config);
        }
        // 更新缓存
        configCache.put(buildKey(group, key), config);
        // 发布配置变更事件
        eventPublisher.publishEvent(new ConfigChangeEvent(this, group, key, value));
        log.info("配置更新成功：{}:{}, 新值: {}", group, key, value);
    }

    /**
     * 根据配置类型验证值是否合法
     */
    private void validateConfigType(String configType, String value) {
        if (configType == null || value == null) {
            return;
        }

        try {
            switch (configType.toUpperCase()) {
                case "INT" -> Integer.parseInt(value);
                case "LONG" -> Long.parseLong(value);
                case "DOUBLE" -> Double.parseDouble(value);
                case "BOOLEAN" -> Boolean.parseBoolean(value);
                case "JSON" -> {
                    // 简单的JSON验证
                    if (!(value.startsWith("{") && value.endsWith("}") || value.startsWith("[") && value.endsWith("]"))) {
                        throw new IllegalArgumentException("不是有效的JSON格式");
                    }
                }
                case "STRING" -> {
                    // 字符串类型不需要验证
                }
                default -> throw new IllegalArgumentException("不支持的配置类型: " + configType);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("配置值 " + value + " 与类型 " + configType + " 不匹配: " + e.getMessage());
        }
    }

    /**
     * 根据值自动检测配置类型
     */
    private String detectConfigType(String value) {
        if (value == null || value.isEmpty()) {
            return "STRING";
        }
        // 布尔类型
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            return "BOOLEAN";
        }
        // JSON类型
        String trimmed = value.trim();
        if ((trimmed.startsWith("{") && trimmed.endsWith("}")) ||
            (trimmed.startsWith("[") && trimmed.endsWith("]"))) {
            return "JSON";
        }
        // 整数类型
        try {
            Integer.parseInt(value);
            return "INT";
        } catch (NumberFormatException ignored) {}
        // 长整数类型
        try {
            Long.parseLong(value);
            return "LONG";
        } catch (NumberFormatException ignored) {}
        // 浮点数类型
        try {
            Double.parseDouble(value);
            return "DOUBLE";
        } catch (NumberFormatException ignored) {}
        return "STRING";
    }

    /**
     * 删除配置
     */
    public void deleteConfig(String group, String key) {
        LambdaQueryWrapper<EtlSystemConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(EtlSystemConfig::getConfigGroup, group)
                   .eq(EtlSystemConfig::getConfigKey, key);
        systemConfigMapper.delete(queryWrapper);
        configCache.remove(buildKey(group, key));
        log.info("配置删除成功：{}:{}", group, key);
    }

    /**
     * 构建缓存键
     */
    private String buildKey(String group, String key) {
        return group + ":" + key;
    }
}
