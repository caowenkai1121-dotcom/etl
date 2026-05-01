package com.etl.monitor.alert;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 告警消息
 */
@Data
public class AlertMessage {

    /**
     * 消息ID
     */
    private String messageId;

    /**
     * 告警规则ID
     */
    private Long ruleId;

    /**
     * 告警规则名称
     */
    private String ruleName;

    /**
     * 告警类型
     */
    private AlertRule.AlertType alertType;

    /**
     * 告警级别
     */
    private AlertLevel level;

    /**
     * 告警标题
     */
    private String title;

    /**
     * 告警内容
     */
    private String content;

    /**
     * 告警来源
     */
    private String source;

    /**
     * 关联对象类型（任务、数据源等）
     */
    private String targetType;

    /**
     * 关联对象ID
     */
    private Long targetId;

    /**
     * 关联对象名称
     */
    private String targetName;

    /**
     * 扩展属性
     */
    private Map<String, Object> extra;

    /**
     * 告警时间
     */
    private LocalDateTime alertTime;

    /**
     * 告警级别枚举
     */
    public enum AlertLevel {
        INFO("信息", "#909399"),
        WARNING("警告", "#E6A23C"),
        ERROR("错误", "#F56C6C"),
        CRITICAL("严重", "#C45656");

        private final String label;
        private final String color;

        AlertLevel(String label, String color) {
            this.label = label;
            this.color = color;
        }

        public String getLabel() {
            return label;
        }

        public String getColor() {
            return color;
        }
    }

    /**
     * 构建 Markdown 格式消息
     */
    public String toMarkdown() {
        StringBuilder sb = new StringBuilder();
        sb.append("## ").append(title != null ? title : "未知告警").append("\n\n");
        sb.append("**告警级别**: ").append(level != null ? level.getLabel() : "未知").append("\n\n");
        sb.append("**告警类型**: ").append(alertType != null ? alertType.getDescription() : "未知").append("\n\n");
        sb.append("**告警时间**: ").append(alertTime != null ? alertTime : "未知").append("\n\n");
        sb.append("**告警来源**: ").append(source != null ? source : "未知").append("\n\n");

        if (targetName != null) {
            sb.append("**关联对象**: ").append(targetName).append("\n\n");
        }

        sb.append("**详细信息**:\n\n").append(content != null ? content : "").append("\n");

        return sb.toString();
    }

    /**
     * 构建纯文本格式消息
     */
    public String toPlainText() {
        StringBuilder sb = new StringBuilder();
        sb.append("【").append(level != null ? level.getLabel() : "未知").append("】")
          .append(title != null ? title : "未知告警").append("\n");
        sb.append("告警类型: ").append(alertType != null ? alertType.getDescription() : "未知").append("\n");
        sb.append("告警时间: ").append(alertTime != null ? alertTime : "未知").append("\n");
        sb.append("告警来源: ").append(source != null ? source : "未知").append("\n");

        if (targetName != null) {
            sb.append("关联对象: ").append(targetName).append("\n");
        }

        sb.append("详细信息: ").append(content != null ? content : "").append("\n");

        return sb.toString();
    }
}
