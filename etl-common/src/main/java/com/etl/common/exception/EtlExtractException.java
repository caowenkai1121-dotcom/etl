package com.etl.common.exception;

import lombok.Getter;

/**
 * ETL抽取异常
 */
@Getter
public class EtlExtractException extends EtlException {

    public EtlExtractException(ErrorCode errorCode) {
        super(errorCode);
    }

    public EtlExtractException(ErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }

    public EtlExtractException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public EtlExtractException(ErrorCode errorCode, String detail, Throwable cause) {
        super(errorCode, detail, cause);
    }

    /**
     * 查询超时
     */
    public static EtlExtractException queryTimeout(String tableName, Throwable cause) {
        return new EtlExtractException(ErrorCode.EXTRACT_001, tableName, cause);
    }

    /**
     * 表不存在
     */
    public static EtlExtractException tableNotFound(String tableName) {
        return new EtlExtractException(ErrorCode.EXTRACT_002, tableName);
    }

    /**
     * 字段不存在
     */
    public static EtlExtractException columnNotFound(String tableName, String columnName) {
        return new EtlExtractException(ErrorCode.EXTRACT_003, tableName + "." + columnName);
    }
}
