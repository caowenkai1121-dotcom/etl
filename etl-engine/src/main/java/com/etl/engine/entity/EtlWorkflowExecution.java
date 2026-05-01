package com.etl.engine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 工作流执行记录实体
 */
@Data
@TableName("etl_workflow_execution")
public class EtlWorkflowExecution implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 工作流ID
     */
    private Long workflowId;

    /**
     * 执行编号
     */
    private String executionNo;

    /**
     * 触发类型: MANUAL/SCHEDULE
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
     * 状态: RUNNING/SUCCESS/FAILED/STOPPED
     */
    private String status;

    /**
     * 当前执行节点ID
     */
    private String currentNodeId;

    /**
     * 总节点数
     */
    private Integer totalNodes;

    /**
     * 已完成节点数
     */
    private Integer completedNodes;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
