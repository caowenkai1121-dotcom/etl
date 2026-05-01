package com.etl.engine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * ETL系统配置实体
 */
@Data
@TableName("etl_system_config")
public class EtlSystemConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String configGroup;
    private String configKey;
    private String configValue;
    private String configType;  // STRING/INT/LONG/DOUBLE/BOOLEAN/JSON
    private String description;
    private Integer isEditable;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
