package com.etl.common.callback;

/**
 * 同步日志回调接口
 * 用于在同步过程中推送实时日志
 */
public interface SyncLogCallback {

    /**
     * 记录日志
     *
     * @param traceId 全链路追踪ID，可为null
     */
    void log(Long taskId, Long executionId, String traceId, String level, String logType, String tableName, String message);

    /**
     * 记录INFO日志
     */
    default void info(Long taskId, Long executionId, String traceId, String tableName, String message) {
        log(taskId, executionId, traceId, "INFO", "SYNC", tableName, message);
    }

    /**
     * 记录WARN日志
     */
    default void warn(Long taskId, Long executionId, String traceId, String tableName, String message) {
        log(taskId, executionId, traceId, "WARN", "SYNC", tableName, message);
    }

    /**
     * 记录ERROR日志
     */
    default void error(Long taskId, Long executionId, String traceId, String tableName, String message) {
        log(taskId, executionId, traceId, "ERROR", "ERROR", tableName, message);
    }

    /**
     * 更新进度
     *
     * @param traceId 全链路追踪ID，可为null
     */
    void progress(Long taskId, Long executionId, String traceId, int progress, long totalRows, long successRows, long failedRows, String status);
}
