package com.etl.engine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("etl_data_validation")
public class EtlDataValidation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Long executionId;
    private String sourceTable;
    private String targetTable;
    private Long sourceDsId;
    private Long targetDsId;
    private String validationType;
    private String status;
    private Long sourceCount;
    private Long targetCount;
    private Integer countMatch;
    private Double sampleRate;
    private Integer sampleSize;
    private Integer matchCount;
    private Integer mismatchCount;
    private Integer missingKeyCount;
    private Integer extraKeyCount;
    private Integer passed;
    private String summary;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
