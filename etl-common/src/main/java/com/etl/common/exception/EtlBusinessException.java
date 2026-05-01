package com.etl.common.exception;

import lombok.Getter;

/**
 * ETL业务异常
 */
@Getter
public class EtlBusinessException extends EtlException {

    public EtlBusinessException(ErrorCode errorCode) {
        super(errorCode);
    }

    public EtlBusinessException(ErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }

    public EtlBusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public EtlBusinessException(ErrorCode errorCode, String detail, Throwable cause) {
        super(errorCode, detail, cause);
    }

    /**
     * 通用业务异常
     */
    public static EtlBusinessException of(String message) {
        return new EtlBusinessException(ErrorCode.UNKNOWN, message);
    }
}
