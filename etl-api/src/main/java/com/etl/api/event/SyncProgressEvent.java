package com.etl.api.event;

import lombok.Getter;

/**
 * 同步进度事件
 */
@Getter
public class SyncProgressEvent {

    private final Long taskId;
    private final Long executionId;
    private final String traceId;
    private final Integer progress;
    private final Long totalRows;
    private final Long successRows;
    private final Long failedRows;
    private final String status;
    private final long timestamp;

    public SyncProgressEvent(Long taskId, Long executionId, String traceId, Integer progress, Long totalRows,
                             Long successRows, Long failedRows, String status) {
        this.taskId = taskId;
        this.executionId = executionId;
        this.traceId = traceId;
        this.progress = progress;
        this.totalRows = totalRows;
        this.successRows = successRows;
        this.failedRows = failedRows;
        this.status = status;
        this.timestamp = System.currentTimeMillis();
    }
}
