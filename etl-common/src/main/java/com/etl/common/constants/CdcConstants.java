package com.etl.common.constants;

/**
 * CDC相关常量
 */
public class CdcConstants {

    private CdcConstants() {}

    // ============ Debezium相关常量 ============

    /**
     * Debezium Connect默认端口
     */
    public static final int DEBEZIUM_DEFAULT_PORT = 8083;

    /**
     * Debezium Connect默认URL
     */
    public static final String DEBEZIUM_CONNECT_URL = "http://debezium-connect:8083";

    /**
     * MySQL连接器类型
     */
    public static final String CONNECTOR_TYPE_MYSQL = "mysql";

    /**
     * PostgreSQL连接器类型
     */
    public static final String CONNECTOR_TYPE_POSTGRESQL = "postgresql";

    /**
     * MySQL Topic前缀
     */
    public static final String MYSQL_TOPIC_PREFIX = "etl-mysql-";

    /**
     * PostgreSQL Topic前缀
     */
    public static final String PG_TOPIC_PREFIX = "etl-pg-";

    /**
     * 连接器名称前缀
     */
    public static final String CONNECTOR_NAME_PREFIX = "etl-connector-";

    // ============ Kafka相关常量 ============

    /**
     * Kafka默认端口
     */
    public static final String KAFKA_DEFAULT_PORT = "9092";

    /**
     * 默认消费组
     */
    public static final String DEFAULT_CONSUMER_GROUP = "etl-sync-group";

    // ============ Debezium操作类型 ============

    /**
     * Debezium操作类型 - 插入
     */
    public static final String OP_INSERT = "c";

    /**
     * Debezium操作类型 - 更新
     */
    public static final String OP_UPDATE = "u";

    /**
     * Debezium操作类型 - 删除
     */
    public static final String OP_DELETE = "d";

    /**
     * Debezium操作类型 - 读取（快照）
     */
    public static final String OP_READ = "r";

    // ============ 同步状态 ============

    /**
     * 同步状态 - 已停止
     */
    public static final String SYNC_STATUS_STOPPED = "STOPPED";

    /**
     * 同步状态 - 运行中
     */
    public static final String SYNC_STATUS_RUNNING = "RUNNING";

    /**
     * 同步状态 - 错误
     */
    public static final String SYNC_STATUS_ERROR = "ERROR";

    /**
     * 同步状态 - 已暂停
     */
    public static final String SYNC_STATUS_PAUSED = "PAUSED";

    // ============ 已废弃的Canal常量（迁移后可删除） ============

    /**
     * @deprecated 已废弃，使用DEBEZIUM_DEFAULT_PORT
     */
    @Deprecated
    public static final int CANAL_DEFAULT_PORT = 11111;

    /**
     * @deprecated 已废弃
     */
    @Deprecated
    public static final String CANAL_DESTINATION_PREFIX = "example";
}
