package com.etl.common.exception;

import lombok.Getter;

/**
 * ETL系统错误码枚举
 * 包含各领域错误码定义及可重试标识
 */
@Getter
public enum ErrorCode {

    // 连接相关错误
    CONNECTION_001("连接超时", true),
    CONNECTION_002("认证失败", false),
    CONNECTION_003("连接池耗尽", true),
    CONNECTION_004("网络中断", true),

    // 抽取相关错误
    EXTRACT_001("查询超时", true),
    EXTRACT_002("表不存在", false),
    EXTRACT_003("字段不存在", false),

    // 转换相关错误
    TRANSFORM_001("转换规则执行失败", false),
    TRANSFORM_002("脱敏失败", false),

    // 加载相关错误
    LOAD_001("主键冲突", true),
    LOAD_002("字段类型不兼容", false),
    LOAD_003("写入超时", true),

    // 配置相关错误
    CONFIG_001("配置缺失", false),
    CONFIG_002("配置值非法", false),

    // 调度相关错误
    SCHEDULE_001("任务依赖循环", false),
    SCHEDULE_002("Cron表达式无效", false),

    // 通用错误
    UNKNOWN("ETL_ERROR", "未知错误", false);

    private final String code;
    private final String message;
    private final boolean retryable;

    ErrorCode(String message, boolean retryable) {
        this.code = name();
        this.message = message;
        this.retryable = retryable;
    }

    ErrorCode(String code, String message, boolean retryable) {
        this.code = code;
        this.message = message;
        this.retryable = retryable;
    }
}
