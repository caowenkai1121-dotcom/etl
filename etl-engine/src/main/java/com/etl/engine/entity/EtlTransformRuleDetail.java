package com.etl.engine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("etl_transform_rule_detail")
public class EtlTransformRuleDetail {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long stageId;
    private String ruleName;
    private String ruleType;
    private Integer sortOrder;
    private Integer enabled;
    private Integer stopOnError;
    private String sourceField;
    private String targetField;
    private String ruleConfig;
    private String filterExpression;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
