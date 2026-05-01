package com.etl.engine.pipeline;

/**
 * 分片信息
 * 标识数据的一个水平分片
 */
public class Shard {

    private final int shardId;
    private final int totalShards;
    private final String shardKey;
    private final Object shardLowerBound;
    private final Object shardUpperBound;

    public Shard(int shardId, int totalShards, String shardKey, Object shardLowerBound, Object shardUpperBound) {
        this.shardId = shardId;
        this.totalShards = totalShards;
        this.shardKey = shardKey;
        this.shardLowerBound = shardLowerBound;
        this.shardUpperBound = shardUpperBound;
    }

    public int getShardId() { return shardId; }
    public int getTotalShards() { return totalShards; }
    public String getShardKey() { return shardKey; }
    public Object getShardLowerBound() { return shardLowerBound; }
    public Object getShardUpperBound() { return shardUpperBound; }

    public String getRangeCondition() {
        StringBuilder sb = new StringBuilder();
        if (shardLowerBound != null) {
            sb.append(shardKey).append(" >= ?");
        }
        if (shardUpperBound != null) {
            if (!sb.isEmpty()) sb.append(" AND ");
            sb.append(shardKey).append(" < ?");
        }
        return sb.toString();
    }

    public Object[] getRangeParams() {
        if (shardLowerBound != null && shardUpperBound != null) {
            return new Object[]{shardLowerBound, shardUpperBound};
        } else if (shardLowerBound != null) {
            return new Object[]{shardLowerBound};
        } else if (shardUpperBound != null) {
            return new Object[]{shardUpperBound};
        }
        return new Object[0];
    }
}
