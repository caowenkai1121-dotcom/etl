package com.etl.engine.pipeline;

import java.util.List;

/**
 * 分片策略接口
 * 定义如何将数据拆分为多个分片
 */
public interface ShardingStrategy {

    /** 策略名称 */
    String getName();

    /** 计算分片 */
    List<Shard> computeShards(PipelineContext context, String tableName, int totalShards) throws Exception;

    /** 默认分片数 */
    default int getDefaultShardCount() {
        return 4;
    }
}
