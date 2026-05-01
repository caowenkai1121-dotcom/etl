package com.etl.datasource.connector.nosql;

import com.etl.common.domain.ColumnInfo;
import com.etl.common.domain.TableInfo;
import com.etl.common.enums.DataSourceType;
import com.etl.datasource.connector.DatabaseConnector;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * MongoDB 数据库连接器
 */
@Slf4j
public class MongoDBConnector implements DatabaseConnector {

    private final Long datasourceId;
    private final String host;
    private final Integer port;
    private final String databaseName;
    private final String username;
    private final String password;
    private final String extraConfig;

    private MongoClient mongoClient;
    private MongoDatabase database;

    public MongoDBConnector(Long datasourceId, String host, Integer port, String databaseName,
                            String username, String password, String charset, String extraConfig) {
        this.datasourceId = datasourceId;
        this.host = host;
        this.port = port != null ? port : 27017;
        this.databaseName = databaseName;
        this.username = username;
        this.password = password;
        this.extraConfig = extraConfig;
    }

    @Override
    public java.sql.Connection getConnection() throws Exception {
        throw new UnsupportedOperationException("MongoDB 使用 MongoClient，不支持 JDBC Connection");
    }

    /**
     * 获取 MongoDB 客户端
     */
    public MongoClient getMongoClient() {
        if (mongoClient == null) {
            String connectionString = buildConnectionString();
            mongoClient = MongoClients.create(connectionString);
            log.info("MongoDB 连接成功: {}", connectionString.replaceAll("://[^:]+:[^@]+@", "://***:***@"));
        }
        return mongoClient;
    }

    /**
     * 获取数据库实例
     */
    public MongoDatabase getDatabase() {
        if (database == null) {
            database = getMongoClient().getDatabase(databaseName);
        }
        return database;
    }

    private String buildConnectionString() {
        StringBuilder sb = new StringBuilder("mongodb://");
        if (username != null && !username.isEmpty()) {
            sb.append(username);
            if (password != null && !password.isEmpty()) {
                sb.append(":").append(password);
            }
            sb.append("@");
        }
        sb.append(host).append(":").append(port);
        sb.append("/").append(databaseName);
        // 添加连接池选项
        sb.append("?maxPoolSize=10&minPoolSize=1&maxIdleTimeMS=60000");
        if (extraConfig != null && !extraConfig.isEmpty()) {
            sb.append("&").append(extraConfig);
        }
        return sb.toString();
    }

    @Override
    public boolean testConnection() {
        try {
            getMongoClient().listDatabaseNames().first();
            return true;
        } catch (Exception e) {
            log.error("MongoDB 连接测试失败", e);
            return false;
        }
    }

    @Override
    public List<String> listDatabases() throws Exception {
        List<String> databases = new ArrayList<>();
        for (String name : getMongoClient().listDatabaseNames()) {
            databases.add(name);
        }
        return databases;
    }

    @Override
    public List<TableInfo> getTables() throws Exception {
        return getTables(databaseName);
    }

    @Override
    public List<TableInfo> getTables(String database) throws Exception {
        List<TableInfo> tables = new ArrayList<>();
        MongoDatabase db = getMongoClient().getDatabase(database);
        for (String collectionName : db.listCollectionNames()) {
            TableInfo tableInfo = new TableInfo();
            tableInfo.setTableName(collectionName);
            tableInfo.setTableType("COLLECTION");
            tableInfo.setSchemaName(database);
            tables.add(tableInfo);
        }
        return tables;
    }

    @Override
    public TableInfo getTableInfo(String tableName) throws Exception {
        TableInfo tableInfo = new TableInfo();
        tableInfo.setTableName(tableName);
        tableInfo.setTableType("COLLECTION");
        tableInfo.setSchemaName(databaseName);
        tableInfo.setColumns(getColumns(tableName));
        return tableInfo;
    }

    @Override
    public List<ColumnInfo> getColumns(String tableName) throws Exception {
        // MongoDB 是 Schema-less 的，通过采样文档推断字段
        List<ColumnInfo> columns = new ArrayList<>();
        try {
            Document sampleDoc = getDatabase().getCollection(tableName).find().first();
            if (sampleDoc != null) {
                int ordinal = 1;
                for (String key : sampleDoc.keySet()) {
                    ColumnInfo column = new ColumnInfo();
                    column.setColumnName(key);
                    column.setColumnType(getBsonTypeName(sampleDoc.get(key)));
                    column.setNullable(true);
                    column.setOrdinalPosition(ordinal++);
                    columns.add(column);
                }
            }
        } catch (Exception e) {
            log.warn("获取 MongoDB 字段信息失败: {}", e.getMessage());
        }
        return columns;
    }

    private String getBsonTypeName(Object value) {
        if (value == null) return "null";
        if (value instanceof String) return "string";
        if (value instanceof Integer) return "int32";
        if (value instanceof Long) return "int64";
        if (value instanceof Double) return "double";
        if (value instanceof Boolean) return "boolean";
        if (value instanceof java.util.Date) return "date";
        if (value instanceof List) return "array";
        if (value instanceof Document) return "object";
        return "unknown";
    }

    @Override
    public List<String> getPrimaryKeys(String tableName) throws Exception {
        // MongoDB 默认主键是 _id
        return List.of("_id");
    }

    @Override
    public long getRowCount(String tableName) throws Exception {
        return getDatabase().getCollection(tableName).countDocuments();
    }

    @Override
    public void close() {
        if (mongoClient != null) {
            mongoClient.close();
            mongoClient = null;
            database = null;
        }
    }

    @Override
    public String getDatabaseType() {
        return "MONGODB";
    }

    @Override
    public String getJdbcUrl() {
        return buildConnectionString();
    }

    @Override
    public String getHost() { return host; }

    @Override
    public Integer getPort() { return port; }

    @Override
    public String getDatabaseName() { return databaseName; }

    @Override
    public String getUsername() { return username; }

    @Override
    public String getPassword() { return password; }

    @Override
    public boolean supportsCdc() {
        return true;
    }

    @Override
    public DataSourceType supportsType() {
        return DataSourceType.MONGODB;
    }
}
