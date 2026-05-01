package com.etl.engine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("etl_quality_log")
public class EtlQualityLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Long executionId;
    private String traceId;
    private String ruleName;
    private String ruleType;
    private String tableName;
    private String fieldName;
    private String fieldValue;
    private String expectedValue;
    private String actualValue;
    private String severity;
    private String status;
    private String errorMessage;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
