package com.etl.monitor.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 告警规则实体
 */
@Data
@TableName("etl_alert_rule")
public class EtlAlertRule implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 规则名称
     */
    @TableField("name")
    private String ruleName;

    /**
     * 告警类型
     */
    private String alertType;

    /**
     * 规则描述
     */
    private String description;

    /**
     * 触发条件表达式(JSON)
     */
    private String conditionExpr;

    /**
     * 阈值
     */
    @TableField(exist = false)
    private Double threshold;

    /**
     * 告警级别
     */
    private String severity;

    /**
     * 通知渠道配置(JSON)
     */
    private String channels;

    /**
     * 通知接收人(JSON数组)
     */
    private String recipients;

    /**
     * 是否启用
     */
    private Integer enabled;

    /**
     * 冷却时间(分钟)
     */
    private Integer cooldownMinutes;

    /**
     * 静默时间(分钟)
     */
    private Integer silenceMinutes;

    /**
     * 升级配置(JSON)
     */
    private String escalationConfig;

    /**
     * 通知渠道配置(JSON)
     */
    private String notificationChannels;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    public String getName() {
        return ruleName;
    }

    public void setName(String name) {
        this.ruleName = name;
    }
}
