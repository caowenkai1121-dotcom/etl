package com.etl.engine.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 工作流节点DTO
 */
@Data
public class WorkflowNodeDto {

    /**
     * 节点ID
     */
    private String id;

    /**
     * 节点类型: SYNC/TRANSFORM/SCRIPT/CONDITION/LOOP
     */
    private String type;

    /**
     * 节点名称
     */
    private String name;

    /**
     * 画布X坐标
     */
    private Integer x;

    /**
     * 画布Y坐标
     */
    private Integer y;

    /**
     * 节点宽度
     */
    private Integer width;

    /**
     * 节点高度
     */
    private Integer height;

    /**
     * 节点配置
     */
    private Map<String, Object> config;
}
