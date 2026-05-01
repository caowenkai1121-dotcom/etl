package com.etl.engine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 工作流定义实体
 */
@Data
@TableName("etl_task_workflow")
public class EtlTaskWorkflow implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 工作流名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 所属文件夹ID
     */
    private Long folderId;

    /**
     * 流程定义JSON(节点和边)
     */
    private String workflowJson;

    /**
     * 状态: DRAFT/PUBLISHED
     */
    private String status;

    /**
     * 发布状态: PENDING/PUBLISHED/UPDATED
     */
    private String publishStatus;

    /**
     * 版本号
     */
    private Integer version;

    /**
     * 调度Cron表达式
     */
    private String cronExpression;

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
