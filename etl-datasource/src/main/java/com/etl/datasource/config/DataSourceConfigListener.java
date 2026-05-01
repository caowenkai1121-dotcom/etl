package com.etl.datasource.config;

import com.etl.common.config.ConfigChangeEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 数据源配置监听器
 * 监听连接池参数变化
 */
@Slf4j
@Component
public class DataSourceConfigListener {

    @EventListener
    public void handleConfigChange(ConfigChangeEvent event) {
        log.info("收到配置变更事件: {}:{} = {}", event.getGroup(), event.getKey(), event.getValue());

        // 监听连接池相关配置
        if ("datasource".equals(event.getGroup())) {
            switch (event.getKey()) {
                case "max_pool_size":
                    // 更新最大连接池大小
                    try {
                        int newSize = Integer.parseInt(event.getValue());
                        log.info("连接池最大连接数配置变更为: {}", newSize);
                        // TODO: 实际更新连接池配置
                    } catch (NumberFormatException e) {
                        log.warn("连接池最大连接数配置值无效: {}", event.getValue());
                    }
                    break;
                case "min_pool_size":
                    // 更新最小连接池大小
                    try {
                        int newSize = Integer.parseInt(event.getValue());
                        log.info("连接池最小连接数配置变更为: {}", newSize);
                        // TODO: 实际更新连接池配置
                    } catch (NumberFormatException e) {
                        log.warn("连接池最小连接数配置值无效: {}", event.getValue());
                    }
                    break;
                case "idle_timeout":
                    // 更新空闲连接超时时间
                    try {
                        long newTimeout = Long.parseLong(event.getValue());
                        log.info("连接池空闲连接超时时间配置变更为: {}ms", newTimeout);
                        // TODO: 实际更新连接池配置
                    } catch (NumberFormatException e) {
                        log.warn("连接池空闲连接超时时间配置值无效: {}", event.getValue());
                    }
                    break;
                case "connection_timeout":
                    // 更新连接超时时间
                    try {
                        long newTimeout = Long.parseLong(event.getValue());
                        log.info("连接池连接超时时间配置变更为: {}ms", newTimeout);
                        // TODO: 实际更新连接池配置
                    } catch (NumberFormatException e) {
                        log.warn("连接池连接超时时间配置值无效: {}", event.getValue());
                    }
                    break;
                case "validation_timeout":
                    // 更新连接验证超时时间
                    try {
                        long newTimeout = Long.parseLong(event.getValue());
                        log.info("连接池连接验证超时时间配置变更为: {}ms", newTimeout);
                        // TODO: 实际更新连接池配置
                    } catch (NumberFormatException e) {
                        log.warn("连接池连接验证超时时间配置值无效: {}", event.getValue());
                    }
                    break;
                case "max_statements":
                    // 更新最大语句缓存数
                    try {
                        int newMax = Integer.parseInt(event.getValue());
                        log.info("连接池最大语句缓存数配置变更为: {}", newMax);
                        // TODO: 实际更新连接池配置
                    } catch (NumberFormatException e) {
                        log.warn("连接池最大语句缓存数配置值无效: {}", event.getValue());
                    }
                    break;
                default:
                    log.debug("未处理的配置变更: {}:{}", event.getGroup(), event.getKey());
            }
        }
    }
}
