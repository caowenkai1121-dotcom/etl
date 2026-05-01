package com.etl.common.exception;

import lombok.Getter;

/**
 * ETL连接异常
 */
@Getter
public class EtlConnectionException extends EtlException {

    public EtlConnectionException(ErrorCode errorCode) {
        super(errorCode);
    }

    public EtlConnectionException(ErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }

    public EtlConnectionException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public EtlConnectionException(ErrorCode errorCode, String detail, Throwable cause) {
        super(errorCode, detail, cause);
    }

    /**
     * 连接超时
     */
    public static EtlConnectionException timeout(String datasourceName, Throwable cause) {
        return new EtlConnectionException(ErrorCode.CONNECTION_001, datasourceName, cause);
    }

    /**
     * 认证失败
     */
    public static EtlConnectionException authFailed(String datasourceName, Throwable cause) {
        return new EtlConnectionException(ErrorCode.CONNECTION_002, datasourceName, cause);
    }

    /**
     * 连接池耗尽
     */
    public static EtlConnectionException poolExhausted(String datasourceName, Throwable cause) {
        return new EtlConnectionException(ErrorCode.CONNECTION_003, datasourceName, cause);
    }

    /**
     * 网络中断
     */
    public static EtlConnectionException networkError(String datasourceName, Throwable cause) {
        return new EtlConnectionException(ErrorCode.CONNECTION_004, datasourceName, cause);
    }
}
