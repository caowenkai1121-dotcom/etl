package com.etl.engine.strategy;

import com.etl.common.domain.SyncPipelineContext;

import java.util.List;
import java.util.Map;

/**
 * 同步策略接口
 */
public interface SyncStrategy {

    /**
     * 获取策略名称
     */
    String getName();

    /**
     * 执行同步
     *
     * @param context 同步上下文
     * @param sourceData 源数据
     * @return 同步的行数
     */
    long execute(SyncPipelineContext context, List<Map<String, Object>> sourceData) throws Exception;

    /**
     * 执行单表同步
     *
     * @param context 同步上下文
     * @param sourceTable 源表名
     * @param targetTable 目标表名
     * @return 同步的行数
     */
    long syncTable(SyncPipelineContext context, String sourceTable, String targetTable) throws Exception;

    /**
     * 是否支持断点续传
     */
    default boolean supportsCheckpoint() {
        return false;
    }
}
