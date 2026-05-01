package com.etl.engine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 失败任务归档实体
 */
@Data
@TableName("etl_failed_task_archive")
public class EtlFailedTaskArchive implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 原始执行记录ID
     */
    private Long originalExecutionId;

    /**
     * 任务ID
     */
    private Long taskId;

    /**
     * 执行编号
     */
    private String executionNo;

    /**
     * 触发类型
     */
    private String triggerType;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 执行时长(毫秒)
     */
    private Long duration;

    /**
     * 状态
     */
    private String status;

    /**
     * 总行数
     */
    private Long totalRows;

    /**
     * 成功行数
     */
    private Long successRows;

    /**
     * 失败行数
     */
    private Long failedRows;

    /**
     * 跳过行数
     */
    private Long skipRows;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 断点信息
     */
    private String checkpoint;

    /**
     * 执行进度
     */
    private BigDecimal progress;

    /**
     * 归档时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime archivedAt;
}
