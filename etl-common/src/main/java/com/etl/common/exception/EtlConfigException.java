package com.etl.common.exception;

import lombok.Getter;

/**
 * ETL配置异常
 */
@Getter
public class EtlConfigException extends EtlException {

    public EtlConfigException(ErrorCode errorCode) {
        super(errorCode);
    }

    public EtlConfigException(ErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }

    public EtlConfigException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public EtlConfigException(ErrorCode errorCode, String detail, Throwable cause) {
        super(errorCode, detail, cause);
    }

    /**
     * 配置缺失
     */
    public static EtlConfigException missing(String configKey) {
        return new EtlConfigException(ErrorCode.CONFIG_001, configKey);
    }

    /**
     * 配置值非法
     */
    public static EtlConfigException invalid(String configKey, String invalidValue) {
        return new EtlConfigException(ErrorCode.CONFIG_002, configKey + "=" + invalidValue);
    }
}
