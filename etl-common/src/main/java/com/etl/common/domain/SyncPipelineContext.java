package com.etl.common.domain;

import com.etl.common.callback.SyncLogCallback;
import lombok.Data;

import java.io.Serializable;

/**
 * 同步流水线上下文（原SyncContext）
 */
@Data
public class SyncPipelineContext implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 任务ID
     */
    private Long taskId;

    /**
     * 执行ID
     */
    private Long executionId;

    /**
     * 执行编号
     */
    private String executionNo;

    /**
     * 源数据源ID
     */
    private Long sourceDsId;

    /**
     * 目标数据源ID
     */
    private Long targetDsId;

    /**
     * 源表名
     */
    private String sourceTable;

    /**
     * 目标表名
     */
    private String targetTable;

    /**
     * 批量大小
     */
    private int batchSize = 1000;

    /**
     * 断点位置
     */
    private String checkpoint;

    /**
     * 总行数
     */
    private long totalRows;

    /**
     * 已处理行数
     */
    private long processedRows;

    /**
     * 成功行数
     */
    private long successRows;

    /**
     * 失败行数
     */
    private long failedRows;

    /**
     * 日志回调（用于实时日志推送）
     */
    private transient SyncLogCallback logCallback;

    /**
     * 增量字段
     */
    private String incrementalField;

    /**
     * 源表列名
     */
    private String sourceColumns;

    /**
     * 数据库类型
     */
    private String dbType;

    /**
     * 全链路追踪ID
     */
    private String traceId;

    /**
     * Pipeline流水线配置(JSON)
     */
    private String pipelineConfig;

    /**
     * 转换流水线ID
     */
    private Long transformPipelineId;

    /**
     * 分片总数
     */
    private int shardTotal = 1;

    /**
     * 是否启用数据校验
     */
    private boolean validationEnabled = false;

    /**
     * 数据校验抽样率
     */
    private double validationSampleRate = 0.1;

    /**
     * 限流器 - 每秒最大读取行数(0=不限)
     */
    private long maxReadRowsPerSecond = 0;

    /**
     * 限流器 - 每秒最大写入行数(0=不限)
     */
    private long maxWriteRowsPerSecond = 0;
}
