package com.etl.engine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("etl_transform_log")
public class EtlTransformLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Long executionId;
    private String traceId;
    private Long stageId;
    private Long ruleId;
    private String ruleName;
    private String ruleType;
    private String tableName;
    private String sourceValue;
    private String targetValue;
    private String status;
    private String errorMessage;
    private Long elapsedMs;
    private Integer recordCount;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
