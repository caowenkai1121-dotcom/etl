package com.etl.engine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 工作流节点执行记录实体
 */
@Data
@TableName("etl_workflow_node_exec")
public class EtlWorkflowNodeExec implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 工作流执行ID
     */
    private Long workflowExecutionId;

    /**
     * 节点ID
     */
    private String nodeId;

    /**
     * 节点名称
     */
    private String nodeName;

    /**
     * 节点类型: SYNC/TRANSFORM/SCRIPT/CONDITION/LOOP
     */
    private String nodeType;

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
     * 状态: PENDING/RUNNING/SUCCESS/FAILED/SKIPPED
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
     * 错误信息
     */
    private String errorMessage;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
