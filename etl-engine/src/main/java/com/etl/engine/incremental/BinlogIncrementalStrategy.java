package com.etl.engine.incremental;

import com.etl.common.domain.SyncPipelineContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;

/**
 * Binlog增量策略实现
 * 简化版，标记为 @ConditionalOnProperty
 * 完整Binlog实现依赖Debezium嵌入式引擎，此处为框架预留
 */
@Component
@ConditionalOnProperty(name = "engine.feature.binlog-incremental.enabled", havingValue = "true")
public class BinlogIncrementalStrategy implements IncrementalStrategy {

    @Override
    public String getType() {
        return "BINLOG";
    }

    @Override
    public String buildQuerySql(SyncPipelineContext context, String lastPosition, String dbType) {
        return null; // binlog不使用SQL查询
    }

    @Override
    public String extractPosition(ResultSet rs, String fieldName) throws Exception {
        return ""; // binlog位点由Debezium管理
    }

    @Override
    public int comparePosition(String pos1, String pos2) {
        return 0; // binlog位点比较逻辑复杂，此处简化处理
    }

    @Override
    public boolean supportsCheckpoint() {
        return true;
    }
}
