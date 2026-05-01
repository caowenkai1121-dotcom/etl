package com.etl.engine.dto;

import com.etl.engine.entity.EtlSyncTask;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 同步任务响应
 */
@Data
public class TaskResponse {

    private Long id;
    private String name;
    private String description;
    private Long sourceDsId;
    private Long targetDsId;
    private String syncMode;
    private String syncScope;
    private String tableConfig;
    private String fieldMapping;
    private String incrementalField;
    private String cronExpression;
    private String syncStrategy;
    private Integer batchSize;
    private Integer parallelThreads;
    private Integer retryTimes;
    private Integer retryInterval;
    private String status;
    private LocalDateTime lastSyncTime;
    private LocalDateTime nextSyncTime;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static TaskResponse from(EtlSyncTask entity) {
        if (entity == null) return null;
        TaskResponse resp = new TaskResponse();
        resp.setId(entity.getId());
        resp.setName(entity.getName());
        resp.setDescription(entity.getDescription());
        resp.setSourceDsId(entity.getSourceDsId());
        resp.setTargetDsId(entity.getTargetDsId());
        resp.setSyncMode(entity.getSyncMode());
        resp.setSyncScope(entity.getSyncScope());
        resp.setTableConfig(entity.getTableConfig());
        resp.setFieldMapping(entity.getFieldMapping());
        resp.setIncrementalField(entity.getIncrementalField());
        resp.setCronExpression(entity.getCronExpression());
        resp.setSyncStrategy(entity.getSyncStrategy());
        resp.setBatchSize(entity.getBatchSize());
        resp.setParallelThreads(entity.getParallelThreads());
        resp.setRetryTimes(entity.getRetryTimes());
        resp.setRetryInterval(entity.getRetryInterval());
        resp.setStatus(entity.getStatus());
        resp.setLastSyncTime(entity.getLastSyncTime());
        resp.setNextSyncTime(entity.getNextSyncTime());
        resp.setCreatedBy(entity.getCreatedBy());
        resp.setCreatedAt(entity.getCreatedAt());
        resp.setUpdatedAt(entity.getUpdatedAt());
        return resp;
    }
}
