package com.etl.engine.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 工作流创建请求
 */
@Data
public class WorkflowCreateRequest {

    /**
     * 工作流名称
     */
    @NotBlank(message = "工作流名称不能为空")
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
     * 流程定义JSON
     */
    @NotBlank(message = "流程定义不能为空")
    private String workflowJson;

    /**
     * Cron表达式
     */
    private String cronExpression;
}
