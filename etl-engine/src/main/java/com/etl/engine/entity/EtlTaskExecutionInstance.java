package com.etl.engine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务执行实例表
 */
@Data
@TableName("etl_task_execution_instance")
public class EtlTaskExecutionInstance {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;

    private Integer version;

    private String status;

    private String triggerType;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Long duration;

    private Long rowCount;

    private String errorMsg;

    private LocalDateTime createdAt;
}
