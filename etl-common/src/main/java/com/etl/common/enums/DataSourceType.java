package com.etl.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 数据源类型枚举
 */
@Getter
@AllArgsConstructor
public enum DataSourceType {

    // 关系型数据库
    MYSQL("MYSQL", "MySQL数据库", "com.mysql.cj.jdbc.Driver"),
    POSTGRESQL("POSTGRESQL", "PostgreSQL数据库", "org.postgresql.Driver"),
    ORACLE("ORACLE", "Oracle数据库", "oracle.jdbc.OracleDriver"),
    SQLSERVER("SQLSERVER", "SQL Server数据库", "com.microsoft.sqlserver.jdbc.SQLServerDriver"),
    DORIS("DORIS", "Apache Doris", "com.mysql.cj.jdbc.Driver"),

    // NoSQL 数据库
    MONGODB("MONGODB", "MongoDB数据库", "com.mongodb.client.MongoClient"),
    ELASTICSEARCH("ELASTICSEARCH", "Elasticsearch搜索引擎", "org.elasticsearch.client.RestHighLevelClient"),

    // 大数据存储
    CLICKHOUSE("CLICKHOUSE", "ClickHouse数据库", "com.clickhouse.jdbc.ClickHouseDriver"),
    HIVE("HIVE", "Apache Hive数据仓库", "org.apache.hive.jdbc.HiveDriver"),

    // 缓存和消息队列
    REDIS("REDIS", "Redis缓存", "redis.clients.jedis.Jedis"),
    KAFKA("KAFKA", "Kafka消息队列", "org.apache.kafka.clients.KafkaConsumer");

    private final String code;
    private final String description;
    private final String driverClass;

    public static DataSourceType fromCode(String code) {
        for (DataSourceType type : values()) {
            if (type.getCode().equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown DataSourceType: " + code);
    }
}
