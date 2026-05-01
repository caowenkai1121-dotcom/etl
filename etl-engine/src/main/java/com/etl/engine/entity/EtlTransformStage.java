package com.etl.engine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("etl_transform_stage")
public class EtlTransformStage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long pipelineId;
    private String stageName;
    private Integer stageOrder;
    private String stageType;
    private Integer enabled;
    private Integer stopOnError;
    private String description;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
