package com.etl.monitor.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 告警记录实体
 */
@Data
@TableName("etl_alert_record")
public class EtlAlertRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 规则ID
     */
    private Long ruleId;

    /**
     * 告警类型
     */
    private String alertType;

    /**
     * 告警级别
     */
    private String severity;

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
     * 关联对象ID
     */
    private Long targetId;

    /**
     * 关联对象名称
     */
    private String targetName;

    /**
     * 状态
     */
    private String status;

    /**
     * 发送时间
     */
    private LocalDateTime sentAt;

    /**
     * 解决时间
     */
    private LocalDateTime resolvedAt;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
