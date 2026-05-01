package com.etl.engine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DAG节点配置实体
 */
@Data
@TableName("etl_dag_node")
public class EtlDagNode {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long dagId;

    private String nodeId;

    private String nodeType;

    private String nodeName;

    private Integer positionX;

    private Integer positionY;

    private String config;

    private String inputSchema;

    private String outputSchema;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}