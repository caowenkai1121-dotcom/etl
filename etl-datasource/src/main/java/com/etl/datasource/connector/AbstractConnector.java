package com.etl.datasource.connector;

import cn.hutool.core.util.StrUtil;
import com.etl.common.domain.ColumnInfo;
import com.etl.common.domain.TableInfo;
import com.etl.common.enums.DataSourceType;
import com.etl.common.utils.EncryptionUtil;
import com.etl.datasource.metadata.MetadataCacheManager;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据库连接器抽象类
 */
@Slf4j
@Getter
public abstract class AbstractConnector implements DatabaseConnector {

    protected Long datasourceId;
    protected String host;
    protected Integer port;
    protected String databaseName;
    protected String username;
    protected String password;
    protected String charset;
    protected String extraConfig;
    protected DataSourceType dataSourceType;
    protected Connection connection;
    protected MetadataCacheManager metadataCacheManager;

    public AbstractConnector(Long datasourceId, String host, Integer port, String databaseName,
                             String username, String password, String charset, String extraConfig) {
        this.datasourceId = datasourceId;
        this.host = host;
        this.port = port;
        this.databaseName = databaseName;
        this.username = username;
        this.password = password;  // 密码已由Service层解密
        this.charset = StrUtil.isBlank(charset) ? "utf8mb4" : charset;
        this.extraConfig = extraConfig;
    }

    // getConnection() 由子类实现
    @Override
    public abstract Connection getConnection() throws Exception;

    @Override
    public boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (Exception e) {
            log.error("测试连接失败: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                log.error("关闭连接失败", e);
            }
            connection = null;
        }
    }

    @Override
    public List<TableInfo> getTables() throws Exception {
        List<TableInfo> tables = new ArrayList<>();
        try (Connection conn = getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            String schema = getSchemaPattern();
            String[] types = {"TABLE", "VIEW"};

            try (ResultSet rs = metaData.getTables(databaseName, schema, "%", types)) {
                while (rs.next()) {
                    TableInfo tableInfo = new TableInfo();
                    tableInfo.setTableName(rs.getString("TABLE_NAME"));
                    tableInfo.setTableComment(rs.getString("REMARKS"));
                    tableInfo.setTableType(rs.getString("TABLE_TYPE"));
                    tableInfo.setSchemaName(schema);
                    tables.add(tableInfo);
                }
            }
        }
        return tables;
    }

    @Override
    public TableInfo getTableInfo(String tableName) throws Exception {
        // 先查缓存
        if (metadataCacheManager != null) {
            TableInfo cached = metadataCacheManager.getTableInfo(datasourceId, databaseName, tableName);
            if (cached != null) {
                return cached;
            }
        }

        try (Connection conn = getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            String schema = getSchemaPattern();

            try (ResultSet rs = metaData.getTables(databaseName, schema, tableName, new String[]{"TABLE"})) {
                if (rs.next()) {
                    TableInfo tableInfo = new TableInfo();
                    tableInfo.setTableName(rs.getString("TABLE_NAME"));
                    tableInfo.setTableComment(rs.getString("REMARKS"));
                    tableInfo.setTableType(rs.getString("TABLE_TYPE"));
                    tableInfo.setSchemaName(schema);
                    tableInfo.setColumns(getColumns(tableName));
                    tableInfo.setPrimaryKeys(getPrimaryKeys(tableName));

                    // 写入缓存
                    if (metadataCacheManager != null) {
                        metadataCacheManager.putTableInfo(datasourceId, databaseName, tableName, tableInfo);
                    }

                    return tableInfo;
                }
            }
        }
        return null;
    }

    @Override
    public List<ColumnInfo> getColumns(String tableName) throws Exception {
        // 先查缓存
        if (metadataCacheManager != null) {
            List<ColumnInfo> cached = metadataCacheManager.getColumns(datasourceId, databaseName, tableName);
            if (cached != null) {
                return cached;
            }
        }

        List<ColumnInfo> columns = new ArrayList<>();
        try (Connection conn = getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            String schema = getSchemaPattern();

            try (ResultSet rs = metaData.getColumns(databaseName, schema, tableName, null)) {
                while (rs.next()) {
                    ColumnInfo column = new ColumnInfo();
                    column.setColumnName(rs.getString("COLUMN_NAME"));
                    column.setColumnType(rs.getString("TYPE_NAME"));
                    column.setColumnLength(rs.getInt("COLUMN_SIZE"));
                    column.setDecimalDigits(rs.getInt("DECIMAL_DIGITS"));
                    column.setNullable("YES".equalsIgnoreCase(rs.getString("IS_NULLABLE")));
                    column.setDefaultValue(rs.getString("COLUMN_DEF"));
                    column.setColumnComment(rs.getString("REMARKS"));
                    column.setOrdinalPosition(rs.getInt("ORDINAL_POSITION"));
                    column.setAutoIncrement("YES".equalsIgnoreCase(rs.getString("IS_AUTOINCREMENT")));
                    columns.add(column);
                }
            }

            // 设置主键标识
            List<String> primaryKeys = getPrimaryKeys(tableName);
            for (ColumnInfo column : columns) {
                column.setPrimaryKey(primaryKeys.contains(column.getColumnName()));
            }

            // 写入缓存
            if (metadataCacheManager != null) {
                metadataCacheManager.putColumns(datasourceId, databaseName, tableName, columns);
            }
        }
        return columns;
    }

    @Override
    public List<String> getPrimaryKeys(String tableName) throws Exception {
        // 先查缓存
        if (metadataCacheManager != null) {
            List<String> cached = metadataCacheManager.getPrimaryKeys(datasourceId, databaseName, tableName);
            if (cached != null) {
                return cached;
            }
        }

        List<String> primaryKeys = new ArrayList<>();
        try (Connection conn = getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            String schema = getSchemaPattern();

            try (ResultSet rs = metaData.getPrimaryKeys(databaseName, schema, tableName)) {
                while (rs.next()) {
                    primaryKeys.add(rs.getString("COLUMN_NAME"));
                }
            }
        }

        // 写入缓存
        if (metadataCacheManager != null) {
            metadataCacheManager.putPrimaryKeys(datasourceId, databaseName, tableName, primaryKeys);
        }

        return primaryKeys;
    }

    /**
     * 设置元数据缓存管理器
     */
    public void setMetadataCacheManager(MetadataCacheManager manager) {
        this.metadataCacheManager = manager;
    }

    @Override
    public long getRowCount(String tableName) throws Exception {
        // 使用参数化的方式构建SQL，避免SQL注入风险
        String quotedTable = quoteIdentifier(tableName);
        // 验证表名格式（只允许字母、数字、下划线）
        if (!tableName.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) {
            throw new IllegalArgumentException("无效的表名: " + tableName);
        }
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + quotedTable)) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        }
        return 0;
    }

    @Override
    public DataSourceType supportsType() {
        return dataSourceType;
    }

    /**
     * 获取Schema模式
     */
    protected abstract String getSchemaPattern();

    /**
     * 获取引号标识符
     */
    protected abstract String quoteIdentifier(String identifier);
}
