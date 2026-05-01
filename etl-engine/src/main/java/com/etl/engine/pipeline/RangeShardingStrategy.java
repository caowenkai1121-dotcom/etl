package com.etl.engine.pipeline;

import java.util.ArrayList;
import java.util.List;

/**
 * 基于主键范围的分片策略
 */
public class RangeShardingStrategy implements ShardingStrategy {

    @Override
    public String getName() {
        return "RANGE";
    }

    @Override
    public List<Shard> computeShards(PipelineContext context, String tableName, int totalShards) throws Exception {
        // 查询主键最小值和最大值
        String sql = "SELECT MIN(id) as minId, MAX(id) as maxId FROM " + tableName;
        // 实际实现需要从 DataSource 获取连接执行查询
        // 简化处理：生成均匀分片占位
        List<Shard> shards = new ArrayList<>();
        long minId = 0, maxId = 1000000; // 占位，实际应由查询决定

        long step = (maxId - minId) / totalShards;
        for (int i = 0; i < totalShards; i++) {
            long lower = minId + i * step;
            long upper = (i == totalShards - 1) ? maxId + 1 : minId + (i + 1) * step;
            shards.add(new Shard(i, totalShards, "id", lower, upper));
        }
        return shards;
    }

    @Override
    public int getDefaultShardCount() {
        return 4;
    }
}
