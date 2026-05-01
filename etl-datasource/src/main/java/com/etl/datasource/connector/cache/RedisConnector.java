package com.etl.datasource.connector.cache;

import com.etl.common.domain.ColumnInfo;
import com.etl.common.domain.TableInfo;
import com.etl.common.enums.DataSourceType;
import com.etl.datasource.connector.DatabaseConnector;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Redis 缓存连接器
 */
@Slf4j
public class RedisConnector implements DatabaseConnector {

    private final Long datasourceId;
    private final String host;
    private final Integer port;
    private final String password;
    private final Integer database;

    private Jedis jedis;

    public RedisConnector(Long datasourceId, String host, Integer port, String databaseName,
                          String username, String password, String charset, String extraConfig) {
        this.datasourceId = datasourceId;
        this.host = host;
        this.port = port != null ? port : 6379;
        this.password = password;
        // 解析数据库编号，默认为0
        int dbIndex = 0;
        if (databaseName != null && !databaseName.isEmpty()) {
            try {
                dbIndex = Integer.parseInt(databaseName);
            } catch (NumberFormatException e) {
                log.warn("Redis databaseName 解析失败，使用默认值0: {}", databaseName);
            }
        }
        this.database = dbIndex;
    }

    private Jedis getJedis() {
        if (jedis == null) {
            jedis = new Jedis(host, port);
            if (password != null && !password.isEmpty()) {
                jedis.auth(password);
            }
            jedis.select(database);
            log.info("Redis 连接成功: {}:{}", host, port);
        }
        return jedis;
    }

    @Override
    public java.sql.Connection getConnection() throws Exception {
        throw new UnsupportedOperationException("Redis 使用 Jedis 客户端，不支持 JDBC Connection");
    }

    @Override
    public boolean testConnection() {
        try {
            String pong = getJedis().ping();
            return "PONG".equalsIgnoreCase(pong);
        } catch (JedisConnectionException e) {
            log.error("Redis 连接测试失败", e);
            return false;
        }
    }

    @Override
    public List<String> listDatabases() throws Exception {
        // Redis 数据库编号 0-15
        List<String> databases = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            databases.add(String.valueOf(i));
        }
        return databases;
    }

    @Override
    public List<TableInfo> getTables() throws Exception {
        // Redis 以 key 前缀作为"表"的概念
        List<TableInfo> tables = new ArrayList<>();
        Set<String> keys = getJedis().keys("*");

        // 按 key 前缀分组
        java.util.Map<String, Integer> prefixCount = new java.util.HashMap<>();
        for (String key : keys) {
            String prefix = getKeyPrefix(key);
            prefixCount.merge(prefix, 1, Integer::sum);
        }

        for (java.util.Map.Entry<String, Integer> entry : prefixCount.entrySet()) {
            TableInfo tableInfo = new TableInfo();
            tableInfo.setTableName(entry.getKey());
            tableInfo.setTableType("KEY_PREFIX");
            tableInfo.setSchemaName(String.valueOf(database));
            tables.add(tableInfo);
        }

        return tables;
    }

    private String getKeyPrefix(String key) {
        int colonIndex = key.indexOf(':');
        if (colonIndex > 0) {
            return key.substring(0, colonIndex);
        }
        return key;
    }

    @Override
    public List<TableInfo> getTables(String database) throws Exception {
        return getTables();
    }

    @Override
    public TableInfo getTableInfo(String tableName) throws Exception {
        TableInfo tableInfo = new TableInfo();
        tableInfo.setTableName(tableName);
        tableInfo.setTableType("KEY_PREFIX");
        tableInfo.setSchemaName(String.valueOf(database));
        tableInfo.setColumns(getColumns(tableName));
        return tableInfo;
    }

    @Override
    public List<ColumnInfo> getColumns(String tableName) throws Exception {
        // Redis 的字段根据数据类型推断
        List<ColumnInfo> columns = new ArrayList<>();

        // 默认字段
        ColumnInfo keyColumn = new ColumnInfo();
        keyColumn.setColumnName("key");
        keyColumn.setColumnType("string");
        keyColumn.setPrimaryKey(true);
        keyColumn.setOrdinalPosition(1);
        columns.add(keyColumn);

        ColumnInfo valueColumn = new ColumnInfo();
        valueColumn.setColumnName("value");
        valueColumn.setColumnType("string");
        valueColumn.setOrdinalPosition(2);
        columns.add(valueColumn);

        ColumnInfo typeColumn = new ColumnInfo();
        typeColumn.setColumnName("type");
        typeColumn.setColumnType("string");
        typeColumn.setOrdinalPosition(3);
        columns.add(typeColumn);

        return columns;
    }

    @Override
    public List<String> getPrimaryKeys(String tableName) throws Exception {
        return List.of("key");
    }

    @Override
    public long getRowCount(String tableName) throws Exception {
        // 获取指定前缀的 key 数量
        Set<String> keys = getJedis().keys(tableName + ":*");
        return keys != null ? keys.size() : 0;
    }

    @Override
    public void close() {
        if (jedis != null) {
            jedis.close();
            jedis = null;
        }
    }

    @Override
    public String getDatabaseType() {
        return "REDIS";
    }

    @Override
    public String getJdbcUrl() {
        return String.format("redis://%s:%d/%d", host, port, database);
    }

    @Override
    public String getHost() { return host; }

    @Override
    public Integer getPort() { return port; }

    @Override
    public String getDatabaseName() { return String.valueOf(database); }

    @Override
    public String getUsername() { return null; }

    @Override
    public String getPassword() { return password; }

    @Override
    public DataSourceType supportsType() {
        return DataSourceType.REDIS;
    }
}
