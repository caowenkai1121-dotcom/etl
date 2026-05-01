package com.etl.datasource.connector;

import com.etl.common.enums.DataSourceType;
import com.etl.common.exception.EtlException;
import com.etl.datasource.connector.bigdata.ClickHouseConnector;
import com.etl.datasource.connector.bigdata.HiveConnector;
import com.etl.datasource.connector.cache.RedisConnector;
import com.etl.datasource.connector.mq.KafkaConnector;
import com.etl.datasource.connector.nosql.ElasticsearchConnector;
import com.etl.datasource.connector.nosql.MongoDBConnector;
import com.etl.datasource.connector.relational.OracleConnector;
import com.etl.datasource.connector.relational.SQLServerConnector;
import com.etl.datasource.entity.EtlDatasource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ConnectorFactory {

    private static final Map<Long, DatabaseConnector> CONNECTOR_CACHE = new ConcurrentHashMap<>();

    private static final Map<String, ConnectorSpi> SPI_REGISTRY = new ConcurrentHashMap<>();

    private final Map<DataSourceType, Class<? extends DatabaseConnector>> connectorTypes = new ConcurrentHashMap<>();

    public ConnectorFactory() {
        // 注册内置的连接器类型
        connectorTypes.put(DataSourceType.MYSQL, MySQLConnector.class);
        connectorTypes.put(DataSourceType.POSTGRESQL, PostgreSQLConnector.class);
        connectorTypes.put(DataSourceType.ORACLE, OracleConnector.class);
        connectorTypes.put(DataSourceType.SQLSERVER, SQLServerConnector.class);
        connectorTypes.put(DataSourceType.DORIS, DorisConnector.class);
        connectorTypes.put(DataSourceType.MONGODB, MongoDBConnector.class);
        connectorTypes.put(DataSourceType.ELASTICSEARCH, ElasticsearchConnector.class);
        connectorTypes.put(DataSourceType.CLICKHOUSE, ClickHouseConnector.class);
        connectorTypes.put(DataSourceType.HIVE, HiveConnector.class);
        connectorTypes.put(DataSourceType.REDIS, RedisConnector.class);
        connectorTypes.put(DataSourceType.KAFKA, KafkaConnector.class);

        // 使用 ServiceLoader 自动发现并注册 SPI
        ServiceLoader<ConnectorSpi> spiLoader = ServiceLoader.load(ConnectorSpi.class);
        for (ConnectorSpi spi : spiLoader) {
            String type = spi.getType().toUpperCase();
            SPI_REGISTRY.put(type, spi);
            log.info("已注册连接器SPI: type={}, class={}", type, spi.getClass().getName());
        }
        log.info("ConnectorFactory 初始化完成，已注册 {} 种数据源类型，{} 个SPI实现",
                 connectorTypes.size(), SPI_REGISTRY.size());
    }

    /**
     * 注册自定义SPI实现
     */
    public static void registerSpi(ConnectorSpi spi) {
        String type = spi.getType().toUpperCase();
        SPI_REGISTRY.put(type, spi);
        log.info("已手动注册连接器SPI: type={}, class={}", type, spi.getClass().getName());
    }

    /**
     * 创建连接器
     * 优先从SPI注册表查找，找不到再使用默认实现
     */
    public static DatabaseConnector createConnector(Long datasourceId, String type, String host,
                                                     Integer port, String databaseName,
                                                     String username, String password,
                                                     String charset, String extraConfig) {
        String typeUpper = type.toUpperCase();

        // 优先从SPI查找
        if (SPI_REGISTRY.containsKey(typeUpper)) {
            ConnectorConfig config = ConnectorConfig.builder()
                .datasourceId(datasourceId)
                .type(typeUpper)
                .host(host)
                .port(port)
                .databaseName(databaseName)
                .username(username)
                .password(password)
                .charset(charset)
                .extraConfig(extraConfig)
                .build();

            DatabaseConnector connector = SPI_REGISTRY.get(typeUpper).create(config);
            CONNECTOR_CACHE.put(datasourceId, connector);
            log.info("通过SPI创建数据源连接器: datasourceId={}, type={}", datasourceId, type);
            return connector;
        }

        // SPI未找到，使用现有switch-case（向下兼容）
        DataSourceType dsType = DataSourceType.fromCode(type);

        DatabaseConnector connector = switch (dsType) {
            // 关系型数据库
            case MYSQL -> new MySQLConnector(datasourceId, host, port, databaseName, username, password, charset, extraConfig);
            case POSTGRESQL -> new PostgreSQLConnector(datasourceId, host, port, databaseName, username, password, charset, extraConfig);
            case ORACLE -> new OracleConnector(datasourceId, host, port, databaseName, username, password, charset, extraConfig);
            case SQLSERVER -> new SQLServerConnector(datasourceId, host, port, databaseName, username, password, charset, extraConfig);
            case DORIS -> new DorisConnector(datasourceId, host, port, databaseName, username, password, charset, extraConfig);

            // NoSQL 数据库
            case MONGODB -> new MongoDBConnector(datasourceId, host, port, databaseName, username, password, charset, extraConfig);
            case ELASTICSEARCH -> new ElasticsearchConnector(datasourceId, host, port, databaseName, username, password, charset, extraConfig);

            // 大数据存储
            case CLICKHOUSE -> new ClickHouseConnector(datasourceId, host, port, databaseName, username, password, charset, extraConfig);
            case HIVE -> new HiveConnector(datasourceId, host, port, databaseName, username, password, charset, extraConfig);

            // 缓存和消息队列
            case REDIS -> new RedisConnector(datasourceId, host, port, databaseName, username, password, charset, extraConfig);
            case KAFKA -> new KafkaConnector(datasourceId, host, port, databaseName, username, password, charset, extraConfig);
        };

        CONNECTOR_CACHE.put(datasourceId, connector);
        log.info("通过默认方式创建数据源连接器: datasourceId={}, type={}", datasourceId, type);
        return connector;
    }

    /**
     * 获取已缓存的连接器
     */
    public static DatabaseConnector getConnector(Long datasourceId) {
        DatabaseConnector connector = CONNECTOR_CACHE.get(datasourceId);
        if (connector == null) {
            throw EtlException.datasourceNotFound(datasourceId);
        }
        return connector;
    }

    /**
     * 获取连接器，如果不存在则创建
     */
    public static DatabaseConnector getOrCreateConnector(Long datasourceId, String type, String host,
                                                          Integer port, String databaseName,
                                                          String username, String password,
                                                          String charset, String extraConfig) {
        DatabaseConnector connector = CONNECTOR_CACHE.get(datasourceId);
        if (connector == null) {
            connector = createConnector(datasourceId, type, host, port, databaseName, username, password, charset, extraConfig);
        }
        return connector;
    }

    /**
     * 移除已缓存的连接器
     */
    public static void removeConnector(Long datasourceId) {
        DatabaseConnector connector = CONNECTOR_CACHE.remove(datasourceId);
        if (connector != null) {
            connector.close();
            log.info("移除数据源连接器: datasourceId={}", datasourceId);
        }
    }

    /**
     * 清空所有连接器
     */
    public static void clearAll() {
        CONNECTOR_CACHE.values().forEach(DatabaseConnector::close);
        CONNECTOR_CACHE.clear();
        log.info("清空所有数据源连接器");
    }

    /**
     * 根据数据源类型获取连接器（多源多目标支持）
     */
    public DatabaseConnector getConnector(DataSourceType type) {
        if (!connectorTypes.containsKey(type)) {
            throw new IllegalArgumentException("不支持的数据源类型: " + type);
        }
        try {
            return connectorTypes.get(type).getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("创建连接器实例失败: " + type, e);
        }
    }

    /**
     * 根据数据源实体获取连接器（多源多目标支持）
     */
    public DatabaseConnector getConnector(EtlDatasource ds) {
        DataSourceType type;
        try {
            type = DataSourceType.valueOf(ds.getType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("不支持的数据源类型: " + ds.getType(), e);
        }
        return getConnector(type);
    }

    /**
     * 检查是否支持指定的数据源类型
     */
    public boolean supports(DataSourceType type) {
        return connectorTypes.containsKey(type);
    }

    /**
     * 获取所有支持的数据源类型
     */
    public Set<DataSourceType> supportedTypes() {
        return connectorTypes.keySet();
    }
}
