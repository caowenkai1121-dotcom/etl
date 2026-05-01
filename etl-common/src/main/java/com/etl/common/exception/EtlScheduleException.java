package com.etl.common.exception;

import lombok.Getter;

/**
 * ETL调度异常
 */
@Getter
public class EtlScheduleException extends EtlException {

    public EtlScheduleException(ErrorCode errorCode) {
        super(errorCode);
    }

    public EtlScheduleException(ErrorCode errorCode, String detail) {
        super(errorCode, detail);
    }

    public EtlScheduleException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public EtlScheduleException(ErrorCode errorCode, String detail, Throwable cause) {
        super(errorCode, detail, cause);
    }

    /**
     * 任务依赖循环
     */
    public static EtlScheduleException circularDependency(String cycleDescription) {
        return new EtlScheduleException(ErrorCode.SCHEDULE_001, cycleDescription);
    }

    /**
     * Cron表达式无效
     */
    public static EtlScheduleException cronInvalid(String cronExpression, Throwable cause) {
        return new EtlScheduleException(ErrorCode.SCHEDULE_002, cronExpression, cause);
    }
}
