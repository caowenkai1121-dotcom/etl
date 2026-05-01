package com.etl.monitor.config;

import com.etl.common.config.ConfigChangeEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 监控配置监听器
 * 监听监控缓存TTL变化
 */
@Slf4j
@Component
public class MonitorConfigListener {

    @EventListener
    public void handleConfigChange(ConfigChangeEvent event) {
        log.info("收到配置变更事件: {}:{} = {}", event.getGroup(), event.getKey(), event.getValue());

        // 监听监控相关配置
        if ("monitor".equals(event.getGroup())) {
            switch (event.getKey()) {
                case "cache_ttl_seconds":
                    // 更新缓存TTL
                    try {
                        int newTtl = Integer.parseInt(event.getValue());
                        log.info("监控缓存TTL配置变更为: {}s", newTtl);
                        // TODO: 实际更新缓存TTL配置
                    } catch (NumberFormatException e) {
                        log.warn("监控缓存TTL配置值无效: {}", event.getValue());
                    }
                    break;
                case "alert_check_interval":
                    // 更新告警检查间隔
                    try {
                        int newInterval = Integer.parseInt(event.getValue());
                        log.info("告警检查间隔配置变更为: {}s", newInterval);
                        // TODO: 实际更新告警检查间隔配置
                    } catch (NumberFormatException e) {
                        log.warn("告警检查间隔配置值无效: {}", event.getValue());
                    }
                    break;
                case "statistic_aggregation_interval":
                    // 更新统计聚合间隔
                    try {
                        int newInterval = Integer.parseInt(event.getValue());
                        log.info("统计聚合间隔配置变更为: {}s", newInterval);
                        // TODO: 实际更新统计聚合间隔配置
                    } catch (NumberFormatException e) {
                        log.warn("统计聚合间隔配置值无效: {}", event.getValue());
                    }
                    break;
                case "log_retention_days":
                    // 更新日志保留天数
                    try {
                        int newDays = Integer.parseInt(event.getValue());
                        log.info("日志保留天数配置变更为: {}天", newDays);
                        // TODO: 实际更新日志保留天数配置
                    } catch (NumberFormatException e) {
                        log.warn("日志保留天数配置值无效: {}", event.getValue());
                    }
                    break;
                default:
                    log.debug("未处理的配置变更: {}:{}", event.getGroup(), event.getKey());
            }
        }
    }
}
