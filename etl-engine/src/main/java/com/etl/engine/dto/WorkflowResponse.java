package com.etl.engine.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工作流响应DTO
 */
@Data
public class WorkflowResponse {

    private Long id;

    private String name;

    private String description;

    private Long folderId;

    private String folderName;

    private String workflowJson;

    private String status;

    private String publishStatus;

    private Integer version;

    private String cronExpression;

    private String createBy;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /**
     * 节点数量
     */
    private Integer nodeCount;

    /**
     * 最后执行时间
     */
    private LocalDateTime lastExecuteTime;

    /**
     * 最后执行状态
     */
    private String lastExecuteStatus;

    public static WorkflowResponse from(Object entity) {
        // 转换逻辑
        return new WorkflowResponse();
    }
}
