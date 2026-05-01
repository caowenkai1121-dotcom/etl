package com.etl.engine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 同步日志实体
 */
@Data
@TableName("etl_sync_log")
public class EtlSyncLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;
    private Long executionId;
    private String traceId;
    private String logLevel;
    private String logType;
    private String stageName;
    private Long transformRuleId;
    private String tableName;
    private String message;
    private Integer recordCount;
    private Long elapsedMs;
    private String detailJson;
    private String stackTrace;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
