package com.etl.api.event;

import lombok.Getter;

/**
 * 同步日志事件
 */
@Getter
public class SyncLogEvent {

    private final Long taskId;
    private final Long executionId;
    private final String traceId;
    private final String logLevel;
    private final String logType;
    private final String tableName;
    private final String stageName;
    private final String message;
    private final Long elapsedMs;
    private final Integer recordCount;
    private final long timestamp;

    public SyncLogEvent(Long taskId, Long executionId, String traceId, String logLevel, String logType,
                        String tableName, String stageName, String message, Long elapsedMs, Integer recordCount) {
        this.taskId = taskId;
        this.executionId = executionId;
        this.traceId = traceId;
        this.logLevel = logLevel;
        this.logType = logType;
        this.tableName = tableName;
        this.stageName = stageName;
        this.message = message;
        this.elapsedMs = elapsedMs;
        this.recordCount = recordCount;
        this.timestamp = System.currentTimeMillis();
    }

    public SyncLogEvent(Long taskId, Long executionId, String traceId, String logLevel, String logType,
                        String tableName, String message) {
        this(taskId, executionId, traceId, logLevel, logType, tableName, null, message, null, null);
    }

    public static SyncLogEvent info(Long taskId, Long executionId, String traceId, String message) {
        return new SyncLogEvent(taskId, executionId, traceId, "INFO", "SYNC", null, null, message, null, null);
    }

    public static SyncLogEvent info(Long taskId, Long executionId, String traceId, String tableName, String message) {
        return new SyncLogEvent(taskId, executionId, traceId, "INFO", "SYNC", tableName, null, message, null, null);
    }

    public static SyncLogEvent warn(Long taskId, Long executionId, String traceId, String message) {
        return new SyncLogEvent(taskId, executionId, traceId, "WARN", "SYNC", null, null, message, null, null);
    }

    public static SyncLogEvent error(Long taskId, Long executionId, String traceId, String message) {
        return new SyncLogEvent(taskId, executionId, traceId, "ERROR", "ERROR", null, null, message, null, null);
    }

    public static SyncLogEvent error(Long taskId, Long executionId, String traceId, String tableName, String message) {
        return new SyncLogEvent(taskId, executionId, traceId, "ERROR", "ERROR", tableName, null, message, null, null);
    }
}
