package com.etl.common.exception;

import lombok.Getter;

/**
 * ETL业务异常基类
 */
@Getter
public class EtlException extends RuntimeException {

    private final String code;
    private final boolean retryable;

    public EtlException(String message) {
        super(message);
        this.code = "ETL_ERROR";
        this.retryable = false;
    }

    public EtlException(String code, String message) {
        super(message);
        this.code = code;
        this.retryable = false;
    }

    public EtlException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.retryable = false;
    }

    // 新增带ErrorCode的构造器
    public EtlException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.retryable = errorCode.isRetryable();
    }

    public EtlException(ErrorCode errorCode, String detail) {
        super(errorCode.getMessage() + ": " + detail);
        this.code = errorCode.getCode();
        this.retryable = errorCode.isRetryable();
    }

    public EtlException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.code = errorCode.getCode();
        this.retryable = errorCode.isRetryable();
    }

    public EtlException(ErrorCode errorCode, String detail, Throwable cause) {
        super(errorCode.getMessage() + ": " + detail, cause);
        this.code = errorCode.getCode();
        this.retryable = errorCode.isRetryable();
    }

    /**
     * 数据源连接异常
     */
    public static EtlException connectionFailed(String datasourceName, Throwable cause) {
        return new EtlException(ErrorCode.CONNECTION_004, datasourceName, cause);
    }

    /**
     * 数据源不存在
     */
    public static EtlException datasourceNotFound(Long datasourceId) {
        return new EtlException("DATASOURCE_NOT_FOUND", "数据源不存在: " + datasourceId);
    }

    /**
     * 任务不存在
     */
    public static EtlException taskNotFound(Long taskId) {
        return new EtlException("TASK_NOT_FOUND", "同步任务不存在: " + taskId);
    }

    /**
     * 任务状态异常
     */
    public static EtlException invalidTaskStatus(String currentStatus, String expectedStatus) {
        return new EtlException("INVALID_TASK_STATUS",
            "任务状态异常，当前状态: " + currentStatus + "，期望状态: " + expectedStatus);
    }

    /**
     * 同步执行异常
     */
    public static EtlException syncFailed(String tableName, Throwable cause) {
        return new EtlException("SYNC_FAILED", "表同步失败: " + tableName, cause);
    }

    /**
     * 字段映射异常
     */
    public static EtlException mappingFailed(String fieldName, String reason) {
        return new EtlException("MAPPING_FAILED",
            "字段映射失败: " + fieldName + "，原因: " + reason);
    }

    /**
     * 表结构获取失败
     */
    public static EtlException metadataFailed(String tableName, Throwable cause) {
        return new EtlException("METADATA_FAILED", "获取表结构失败: " + tableName, cause);
    }

    /**
     * CDC异常
     */
    public static EtlException cdcError(String message, Throwable cause) {
        return new EtlException("CDC_ERROR", message, cause);
    }

    /**
     * 配置错误
     */
    public static EtlException configError(String message) {
        return new EtlException(ErrorCode.CONFIG_002, message);
    }
}
