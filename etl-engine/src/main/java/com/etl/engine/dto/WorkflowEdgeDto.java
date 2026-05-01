package com.etl.engine.dto;

import lombok.Data;

/**
 * 工作流边DTO
 */
@Data
public class WorkflowEdgeDto {

    /**
     * 边ID
     */
    private String id;

    /**
     * 源节点ID
     */
    private String source;

    /**
     * 目标节点ID
     */
    private String target;

    /**
     * 源节点锚点
     */
    private String sourceAnchor;

    /**
     * 目标节点锚点
     */
    private String targetAnchor;

    /**
     * 边标签(条件分支条件)
     */
    private String label;
}
