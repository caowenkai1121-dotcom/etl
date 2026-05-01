package com.etl.common.constants;

/**
 * 系统常量
 */
public class SystemConstants {

    private SystemConstants() {}

    /**
     * 默认批量大小
     */
    public static final int DEFAULT_BATCH_SIZE = 1000;

    /**
     * 默认并行线程数
     */
    public static final int DEFAULT_PARALLEL_THREADS = 4;

    /**
     * 默认重试次数
     */
    public static final int DEFAULT_RETRY_TIMES = 3;

    /**
     * 默认重试间隔(秒)
     */
    public static final int DEFAULT_RETRY_INTERVAL = 60;

    /**
     * 日志保留天数
     */
    public static final int LOG_RETENTION_DAYS = 30;

    /**
     * 执行记录保留天数
     */
    public static final int EXECUTION_RETENTION_DAYS = 90;

    /**
     * 执行编号前缀
     */
    public static final String EXECUTION_NO_PREFIX = "EXEC";

    /**
     * 正常状态
     */
    public static final Integer STATUS_NORMAL = 1;

    /**
     * 禁用状态
     */
    public static final Integer STATUS_DISABLED = 0;

    /**
     * 未删除
     */
    public static final Integer NOT_DELETED = 0;

    /**
     * 已删除
     */
    public static final Integer DELETED = 1;
}
