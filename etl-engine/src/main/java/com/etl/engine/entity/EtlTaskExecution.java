package com.etl.engine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 任务执行记录实体
 */
@Data
@TableName("etl_task_execution")
public class EtlTaskExecution implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

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
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
