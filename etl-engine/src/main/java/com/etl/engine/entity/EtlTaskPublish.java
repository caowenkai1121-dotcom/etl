package com.etl.engine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 任务发布记录实体
 */
@Data
@TableName("etl_task_publish")
public class EtlTaskPublish implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 任务ID
     */
    private Long taskId;

    /**
     * 任务类型: WORKFLOW/TASK/API
     */
    private String taskType;

    /**
     * 版本号
     */
    private Integer version;

    /**
     * 发布状态: PENDING/PUBLISHED/UPDATED
     */
    private String publishStatus;

    /**
     * 发布人
     */
    private String publishedBy;

    /**
     * 发布时间
     */
    private LocalDateTime publishedAt;

    /**
     * 变更说明
     */
    private String changeLog;

    /**
     * 配置快照
     */
    private String snapshotConfig;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
