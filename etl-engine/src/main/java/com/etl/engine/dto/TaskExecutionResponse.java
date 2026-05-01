package com.etl.engine.dto;

import com.etl.engine.entity.EtlTaskExecution;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 任务执行记录响应
 */
@Data
public class TaskExecutionResponse {

    private Long id;
    private Long taskId;
    private String executionNo;
    private String triggerType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long duration;
    private String status;
    private Long totalRows;
    private Long successRows;
    private Long failedRows;
    private Long skipRows;
    private String errorMessage;
    private BigDecimal progress;
    private LocalDateTime createdAt;

    public static TaskExecutionResponse from(EtlTaskExecution entity) {
        if (entity == null) return null;
        TaskExecutionResponse resp = new TaskExecutionResponse();
        resp.setId(entity.getId());
        resp.setTaskId(entity.getTaskId());
        resp.setExecutionNo(entity.getExecutionNo());
        resp.setTriggerType(entity.getTriggerType());
        resp.setStartTime(entity.getStartTime());
        resp.setEndTime(entity.getEndTime());
        resp.setDuration(entity.getDuration());
        resp.setStatus(entity.getStatus());
        resp.setTotalRows(entity.getTotalRows());
        resp.setSuccessRows(entity.getSuccessRows());
        resp.setFailedRows(entity.getFailedRows());
        resp.setSkipRows(entity.getSkipRows());
        resp.setErrorMessage(entity.getErrorMessage());
        resp.setProgress(entity.getProgress());
        resp.setCreatedAt(entity.getCreatedAt());
        return resp;
    }
}
