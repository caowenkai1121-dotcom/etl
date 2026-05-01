package com.etl.common.exception;

import lombok.Getter;

/**
 * ETL加载异常
 */
@Getter
public class EtlLoadException extends EtlException {

    public EtlLoadException(ErrorCode errorCode) {
        super(errorCode);
    }

    public EtlLoadException(ErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }

    public EtlLoadException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public EtlLoadException(ErrorCode errorCode, String detail, Throwable cause) {
        super(errorCode, detail, cause);
    }

    /**
     * 主键冲突
     */
    public static EtlLoadException conflict(String tableName, String primaryKey, Throwable cause) {
        return new EtlLoadException(ErrorCode.LOAD_001, tableName + ":" + primaryKey, cause);
    }

    /**
     * 字段类型不兼容
     */
    public static EtlLoadException typeIncompatible(String tableName, String fieldName, Throwable cause) {
        return new EtlLoadException(ErrorCode.LOAD_002, tableName + ":" + fieldName, cause);
    }

    /**
     * 写入超时
     */
    public static EtlLoadException writeTimeout(String tableName, Throwable cause) {
        return new EtlLoadException(ErrorCode.LOAD_003, tableName, cause);
    }
}
