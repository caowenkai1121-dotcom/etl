package com.etl.engine.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.etl.common.constants.CdcConstants;
import com.etl.common.exception.EtlException;
import com.etl.common.utils.EncryptionUtil;
import com.etl.datasource.entity.EtlDatasource;
import com.etl.datasource.service.DatasourceService;
import com.etl.engine.entity.EtlCdcConfig;
import com.etl.engine.mapper.CdcConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * CDC配置服务
 * 管理Debezium连接器配置的CRUD操作
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CdcConfigService extends ServiceImpl<CdcConfigMapper, EtlCdcConfig> {

    private final DatasourceService datasourceService;

    /**
     * 分页查询CDC配置
     */
    public Page<EtlCdcConfig> pageList(Integer pageNum, Integer pageSize, String name, String syncStatus) {
        LambdaQueryWrapper<EtlCdcConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(name), EtlCdcConfig::getName, name)
               .eq(StrUtil.isNotBlank(syncStatus), EtlCdcConfig::getSyncStatus, syncStatus)
               .orderByDesc(EtlCdcConfig::getCreatedAt);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    /**
     * 获取所有启用的CDC配置
     */
    public List<EtlCdcConfig> listEnabled() {
        LambdaQueryWrapper<EtlCdcConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EtlCdcConfig::getStatus, 1)
               .orderByDesc(EtlCdcConfig::getCreatedAt);
        return list(wrapper);
    }

    /**
     * 根据数据源ID获取CDC配置
     */
    public EtlCdcConfig getByDatasourceId(Long datasourceId) {
        LambdaQueryWrapper<EtlCdcConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EtlCdcConfig::getDatasourceId, datasourceId)
               .eq(EtlCdcConfig::getStatus, 1);
        return getOne(wrapper);
    }

    /**
     * 创建CDC配置
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createConfig(EtlCdcConfig config) {
        // 检查数据源是否存在
        EtlDatasource datasource = datasourceService.getById(config.getDatasourceId());
        if (datasource == null) {
            throw EtlException.datasourceNotFound(config.getDatasourceId());
        }

        // 生成连接器名称（如果未指定）
        if (StrUtil.isBlank(config.getConnectorName())) {
            config.setConnectorName(generateConnectorName(config.getConnectorType(), config.getDatasourceId()));
        }

        // 检查连接器名称是否重复
        LambdaQueryWrapper<EtlCdcConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EtlCdcConfig::getConnectorName, config.getConnectorName());
        if (count(wrapper) > 0) {
            throw new RuntimeException("连接器名称已存在: " + config.getConnectorName());
        }

        // 设置连接器类型（如果未指定）
        if (StrUtil.isBlank(config.getConnectorType())) {
            config.setConnectorType(detectConnectorType(datasource));
        }

        // 生成服务器名称（如果未指定）
        if (StrUtil.isBlank(config.getServerName())) {
            config.setServerName(generateServerName(config.getConnectorType()));
        }

        // 加密密码
        if (StrUtil.isNotBlank(config.getDbPassword())) {
            config.setDbPassword(EncryptionUtil.encrypt(config.getDbPassword()));
        }

        // 设置默认值
        if (StrUtil.isBlank(config.getSyncStatus())) {
            config.setSyncStatus(CdcConstants.SYNC_STATUS_STOPPED);
        }
        if (config.getStatus() == null) {
            config.setStatus(1);
        }
        if (StrUtil.isBlank(config.getFilterRegex())) {
            config.setFilterRegex(".*\\..*");
        }
        // 处理extraConfig空字符串
        if (StrUtil.isBlank(config.getExtraConfig())) {
            config.setExtraConfig(null);
        }

        save(config);
        log.info("创建CDC配置成功: id={}, name={}, connectorName={}, type={}",
            config.getId(), config.getName(), config.getConnectorName(), config.getConnectorType());
        return config.getId();
    }

    /**
     * 更新CDC配置
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateConfig(EtlCdcConfig config) {
        EtlCdcConfig existing = getById(config.getId());
        if (existing == null) {
            throw new RuntimeException("CDC配置不存在: " + config.getId());
        }

        // 检查连接器名称是否重复（排除自身）
        if (!existing.getConnectorName().equals(config.getConnectorName())) {
            LambdaQueryWrapper<EtlCdcConfig> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(EtlCdcConfig::getConnectorName, config.getConnectorName())
                   .ne(EtlCdcConfig::getId, config.getId());
            if (count(wrapper) > 0) {
                throw new RuntimeException("连接器名称已存在: " + config.getConnectorName());
            }
        }

        // 如果密码有变化，加密新密码
        if (StrUtil.isNotBlank(config.getDbPassword())
            && !config.getDbPassword().equals(existing.getDbPassword())) {
            config.setDbPassword(EncryptionUtil.encrypt(config.getDbPassword()));
        }

        // 运行中的配置不能修改关键参数
        if (CdcConstants.SYNC_STATUS_RUNNING.equals(existing.getSyncStatus())) {
            if (!existing.getConnectorName().equals(config.getConnectorName()) ||
                !existing.getDatasourceId().equals(config.getDatasourceId())) {
                throw new RuntimeException("CDC任务运行中，不能修改关键配置，请先停止任务");
            }
        }

        updateById(config);
        log.info("更新CDC配置成功: id={}", config.getId());
    }

    /**
     * 删除CDC配置
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteConfig(Long id) {
        EtlCdcConfig config = getById(id);
        if (config == null) {
            throw new RuntimeException("CDC配置不存在: " + id);
        }

        // 运行中的配置不能删除
        if (CdcConstants.SYNC_STATUS_RUNNING.equals(config.getSyncStatus())) {
            throw new RuntimeException("CDC任务运行中，请先停止任务");
        }

        removeById(id);
        log.info("删除CDC配置成功: id={}, connectorName={}", id, config.getConnectorName());
    }

    /**
     * 更新同步状态
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateSyncStatus(Long id, String syncStatus, String errorMessage) {
        EtlCdcConfig config = new EtlCdcConfig();
        config.setId(id);
        config.setSyncStatus(syncStatus);
        config.setErrorMessage(errorMessage);
        if (CdcConstants.SYNC_STATUS_RUNNING.equals(syncStatus)) {
            config.setLastSyncTime(LocalDateTime.now());
        }
        updateById(config);
        log.info("更新CDC同步状态: id={}, status={}", id, syncStatus);
    }

    /**
     * 启用/禁用CDC配置
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        EtlCdcConfig config = getById(id);
        if (config == null) {
            throw new RuntimeException("CDC配置不存在: " + id);
        }

        // 禁用时需要先停止运行中的任务
        if (status == 0 && CdcConstants.SYNC_STATUS_RUNNING.equals(config.getSyncStatus())) {
            throw new RuntimeException("CDC任务运行中，请先停止任务");
        }

        config.setStatus(status);
        updateById(config);
        log.info("更新CDC配置状态: id={}, status={}", id, status);
    }

    /**
     * 获取完整的数据库连接信息
     * 如果CDC配置中有覆盖值，使用覆盖值；否则使用数据源配置
     */
    public DatabaseConnectionInfo getDatabaseConnectionInfo(Long configId) {
        EtlCdcConfig config = getById(configId);
        if (config == null) {
            throw new RuntimeException("CDC配置不存在: " + configId);
        }

        EtlDatasource datasource = datasourceService.getById(config.getDatasourceId());
        if (datasource == null) {
            throw EtlException.datasourceNotFound(config.getDatasourceId());
        }

        DatabaseConnectionInfo info = new DatabaseConnectionInfo();
        info.setHost(StrUtil.isNotBlank(config.getDatabaseHost())
            ? config.getDatabaseHost()
            : datasource.getHost());
        info.setPort(config.getDatabasePort() != null
            ? config.getDatabasePort()
            : datasource.getPort());
        info.setUsername(StrUtil.isNotBlank(config.getDbUsername())
            ? config.getDbUsername()
            : datasource.getUsername());
        info.setPassword(StrUtil.isNotBlank(config.getDbPassword())
            ? EncryptionUtil.decrypt(config.getDbPassword())
            : EncryptionUtil.decrypt(datasource.getPassword()));
        info.setDatabase(datasource.getDatabaseName());

        return info;
    }

    /**
     * 生成连接器名称
     */
    private String generateConnectorName(String connectorType, Long datasourceId) {
        String type = StrUtil.isNotBlank(connectorType) ? connectorType : "mysql";
        return CdcConstants.CONNECTOR_NAME_PREFIX + type + "-" + datasourceId;
    }

    /**
     * 生成服务器名称
     */
    private String generateServerName(String connectorType) {
        if (CdcConstants.CONNECTOR_TYPE_POSTGRESQL.equalsIgnoreCase(connectorType)) {
            return CdcConstants.PG_TOPIC_PREFIX + System.currentTimeMillis();
        }
        return CdcConstants.MYSQL_TOPIC_PREFIX + System.currentTimeMillis();
    }

    /**
     * 根据数据源检测连接器类型
     */
    private String detectConnectorType(EtlDatasource datasource) {
        String dbType = datasource.getType();
        if (dbType != null) {
            dbType = dbType.toUpperCase();
            if ("MYSQL".equals(dbType)) {
                return CdcConstants.CONNECTOR_TYPE_MYSQL;
            } else if ("POSTGRESQL".equals(dbType) || "POSTGRES".equals(dbType)) {
                return CdcConstants.CONNECTOR_TYPE_POSTGRESQL;
            }
        }
        // 默认MySQL
        return CdcConstants.CONNECTOR_TYPE_MYSQL;
    }

    /**
     * 数据库连接信息
     */
    @lombok.Data
    public static class DatabaseConnectionInfo {
        private String host;
        private Integer port;
        private String username;
        private String password;
        private String database;

        public String getAddress() {
            return host + ":" + port;
        }
    }
}
