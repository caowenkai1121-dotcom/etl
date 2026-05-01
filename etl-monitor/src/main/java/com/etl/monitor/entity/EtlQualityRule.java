package com.etl.monitor.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 数据质量规则实体
 */
@Data
@TableName("etl_quality_rule")
public class EtlQualityRule implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联任务ID
     */
    @TableField("task_id")
    private Long taskId;

    /**
     * 规则名称
     */
    @TableField("rule_name")
    private String ruleName;

    /**
     * 规则维度: COMPLETENESS/ACCURACY/CONSISTENCY/TIMELINESS
     */
    @TableField("rule_dimension")
    private String ruleDimension;

    /**
     * 规则类型
     */
    @TableField("rule_type")
    private String ruleType;

    /**
     * 规则配置(JSON)
     */
    @TableField("rule_config")
    private String ruleConfig;

    /**
     * 目标字段
     */
    @TableField("target_field")
    private String targetField;

    /**
     * 表达式
     */
    @TableField("expression")
    private String expression;

    /**
     * 阈值
     */
    @TableField("threshold")
    private Integer threshold;

    /**
     * 严重程度: INFO/WARN/ERROR/CRITICAL
     */
    @TableField("severity")
    private String severity;

    /**
     * 是否启用: 0-禁用 1-启用
     */
    @TableField("enabled")
    private Integer enabled;

    /**
     * 描述
     */
    @TableField("description")
    private String description;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 是否删除: 0-否 1-是
     */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
