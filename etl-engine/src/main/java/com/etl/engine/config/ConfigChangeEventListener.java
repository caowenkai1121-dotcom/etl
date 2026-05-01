package com.etl.engine.config;

import com.etl.common.config.ConfigChangeEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 配置变更监听器
 * 监听线程池大小、批次大小等配置变化
 */
@Slf4j
@Component
public class ConfigChangeEventListener {

    @EventListener
    public void handleConfigChange(ConfigChangeEvent event) {
        log.info("收到配置变更事件: {}:{} = {}", event.getGroup(), event.getKey(), event.getValue());

        // 监听线程池相关配置
        if ("engine".equals(event.getGroup())) {
            switch (event.getKey()) {
                case "thread_pool_size":
                    // 更新线程池大小
                    try {
                        int newSize = Integer.parseInt(event.getValue());
                        log.info("线程池大小配置变更为: {}", newSize);
                        // TODO: 实际更新线程池配置
                    } catch (NumberFormatException e) {
                        log.warn("线程池大小配置值无效: {}", event.getValue());
                    }
                    break;
                case "batch_size":
                    // 更新批次大小
                    try {
                        int newSize = Integer.parseInt(event.getValue());
                        log.info("批次大小配置变更为: {}", newSize);
                        // TODO: 实际更新批次大小配置
                    } catch (NumberFormatException e) {
                        log.warn("批次大小配置值无效: {}", event.getValue());
                    }
                    break;
                case "queue_capacity":
                    // 更新队列容量
                    try {
                        int newCapacity = Integer.parseInt(event.getValue());
                        log.info("队列容量配置变更为: {}", newCapacity);
                        // TODO: 实际更新队列容量配置
                    } catch (NumberFormatException e) {
                        log.warn("队列容量配置值无效: {}", event.getValue());
                    }
                    break;
                case "retries":
                    // 更新重试次数
                    try {
                        int newRetries = Integer.parseInt(event.getValue());
                        log.info("重试次数配置变更为: {}", newRetries);
                        // TODO: 实际更新重试次数配置
                    } catch (NumberFormatException e) {
                        log.warn("重试次数配置值无效: {}", event.getValue());
                    }
                    break;
                default:
                    log.debug("未处理的配置变更: {}:{}", event.getGroup(), event.getKey());
            }
        }
    }
}
