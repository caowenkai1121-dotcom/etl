package com.etl.engine.incremental;

import com.etl.common.domain.SyncPipelineContext;
import java.sql.ResultSet;

public interface IncrementalStrategy {
    String getType();
    String buildQuerySql(SyncPipelineContext context, String lastPosition, String dbType);
    String extractPosition(ResultSet rs, String fieldName) throws Exception;
    int comparePosition(String pos1, String pos2);
    boolean supportsCheckpoint();
}
