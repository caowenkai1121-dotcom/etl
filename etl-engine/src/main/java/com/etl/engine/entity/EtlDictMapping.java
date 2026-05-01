package com.etl.engine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("etl_dict_mapping")
public class EtlDictMapping {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String dictName;
    private String dictCode;
    private String sourceValue;
    private String targetValue;
    private String description;
    private Integer enabled;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
