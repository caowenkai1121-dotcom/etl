package com.etl.engine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文件夹实体
 */
@Data
@TableName("etl_folder")
public class EtlFolder implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 文件夹名称
     */
    private String name;

    /**
     * 父文件夹ID，0表示根目录
     */
    private Long parentId;

    /**
     * 排序序号
     */
    private Integer sortOrder;

    /**
     * 文件夹类型: WORKFLOW/API/TASK
     */
    private String folderType;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新人
     */
    private String updateBy;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除
     */
    @TableLogic
    private Integer deleted;
}
