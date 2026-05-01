package com.etl.engine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 节点执行记录表
 */
@Data
@TableName("etl_node_execution_log")
public class EtlNodeExecutionLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long executionId;

    private String nodeId;

    private String nodeName;

    private String status;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Long inputRows;

    private Long outputRows;

    private Long errorRows;

    private String logContent;

    private LocalDateTime createdAt;
}
