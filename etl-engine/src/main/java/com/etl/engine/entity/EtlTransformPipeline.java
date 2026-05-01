package com.etl.engine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 转换流水线实体
 */
@Data
@TableName("etl_transform_pipeline")
public class EtlTransformPipeline implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 流水线名称
     */
    private String name;

    /**
     * 流水线描述
     */
    private String description;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 创建人
     */
    @TableField(value = "created_by")
    private String createBy;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新人
     */
    @TableField(value = "updated_by")
    private String updateBy;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除
     */
    @TableLogic
    private Integer deleted;
}
