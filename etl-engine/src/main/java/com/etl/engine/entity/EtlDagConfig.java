package com.etl.engine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DAG流程配置实体
 */
@Data
@TableName("etl_dag_config")
public class EtlDagConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;

    private String nodes;

    private String edges;

    private String viewport;

    private Integer version;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}