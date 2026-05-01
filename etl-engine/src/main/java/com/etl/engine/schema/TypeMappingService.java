package com.etl.engine.schema;

import com.etl.common.domain.TableInfo;

public interface TypeMappingService {
    String mapType(String sourceDbType, String sourceType, String targetDbType);
    String getCreateTableDialect(String dbType, TableInfo tableInfo, CreateConfig config);

    @lombok.Data
    class CreateConfig {
        private String[] primaryKeys;
        private String distributionKey;
        private int buckets = 10;
        private int replicationNum = 1;
    }
}
