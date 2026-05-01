package com.etl.monitor.alert;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 告警规则
 */
@Data
public class AlertRule {

    private Long id;
    private String name;
    private String description;
    private boolean enabled;

    /**
     * 规则类型
     */
    private AlertType alertType;

    /**
     * 触发条件
     */
    private AlertCondition condition;

    /**
     * 通知渠道列表
     */
    private String[] channels;

    /**
     * 通知接收者
     */
    private String[] recipients;

    /**
     * 静默时间（秒）
     */
    private int silenceSeconds;

    /**
     * 最后触发时间
     */
    private LocalDateTime lastTriggeredAt;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 告警类型枚举
     */
    public enum AlertType {
        TASK_FAILED("任务失败"),
        TASK_TIMEOUT("任务超时"),
        SYNC_DELAY("同步延迟"),
        SYNC_ERROR("同步异常"),
        CONNECTION_ERROR("连接异常"),
        SYSTEM_ERROR("系统异常");

        private final String description;

        AlertType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 告警条件
     */
    @Data
    public static class AlertCondition {
        /**
         * 比较操作符
         */
        private Operator operator;

        /**
         * 阈值
         */
        private double threshold;

        /**
         * 持续时间（秒）
         */
        private int durationSeconds;

        /**
         * 次数阈值
         */
        private int countThreshold;

        /**
         * 操作符枚举
         */
        public enum Operator {
            GREATER_THAN(">"),
            LESS_THAN("<"),
            EQUAL("=="),
            NOT_EQUAL("!="),
            GREATER_THAN_OR_EQUAL(">="),
            LESS_THAN_OR_EQUAL("<=");

            private final String symbol;

            Operator(String symbol) {
                this.symbol = symbol;
            }

            public String getSymbol() {
                return symbol;
            }

            public boolean evaluate(double actual, double threshold) {
                return switch (this) {
                    case GREATER_THAN -> actual > threshold;
                    case LESS_THAN -> actual < threshold;
                    case EQUAL -> actual == threshold;
                    case NOT_EQUAL -> actual != threshold;
                    case GREATER_THAN_OR_EQUAL -> actual >= threshold;
                    case LESS_THAN_OR_EQUAL -> actual <= threshold;
                };
            }
        }
    }
}
