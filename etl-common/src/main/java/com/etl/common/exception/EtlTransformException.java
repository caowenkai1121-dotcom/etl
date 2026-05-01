package com.etl.common.exception;

import lombok.Getter;

/**
 * ETL转换异常
 */
@Getter
public class EtlTransformException extends EtlException {

    public EtlTransformException(ErrorCode errorCode) {
        super(errorCode);
    }

    public EtlTransformException(ErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }

    public EtlTransformException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public EtlTransformException(ErrorCode errorCode, String detail, Throwable cause) {
        super(errorCode, detail, cause);
    }

    /**
     * 转换规则执行失败
     */
    public static EtlTransformException ruleFailed(String ruleName, String fieldName, Throwable cause) {
        return new EtlTransformException(ErrorCode.TRANSFORM_001, ruleName + ":" + fieldName, cause);
    }

    /**
     * 脱敏失败
     */
    public static EtlTransformException desensitizeFailed(String fieldName, Throwable cause) {
        return new EtlTransformException(ErrorCode.TRANSFORM_002, fieldName, cause);
    }
}
