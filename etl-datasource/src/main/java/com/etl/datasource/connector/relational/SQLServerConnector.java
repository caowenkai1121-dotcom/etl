package com.etl.datasource.connector.relational;

import cn.hutool.core.util.StrUtil;
import com.etl.common.domain.ColumnInfo;
import com.etl.common.domain.TableInfo;
import com.etl.common.enums.DataSourceType;
import com.etl.common.exception.EtlException;
import com.etl.datasource.connector.AbstractConnector;
import com.etl.datasource.connector.ConnectionPoolManager;
import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * SQL Server 数据库连接器
 * 使用 HikariCP 连接池管理连接
 */
@Slf4j
public class SQLServerConnector extends AbstractConnector {

    private static final String JDBC_URL_TEMPLATE = "jdbc:sqlserver://%s:%d;databaseName=%s";

    // 连接池名称
    private final String poolName;

    public SQLServerConnector(Long datasourceId, String host, Integer port, String databaseName,
                              String username, String password, String charset, String extraConfig) {
        super(datasourceId, host, port, databaseName, username, password, charset, extraConfig);
        this.dataSourceType = DataSourceType.SQLSERVER;
        this.poolName = "sqlserver-pool-" + datasourceId;
    }

    @Override
    public Connection getConnection() throws Exception {
        try {
            String url = getJdbcUrl();
            // 使用线程安全的方法获取连接（自动创建连接池）
            Connection conn = ConnectionPoolManager.getOrCreateConnection(
                poolName, url, username, password, dataSourceType.getDriverClass());

            // 设置事务隔离级别为 READ_COMMITTED（避免读取阻塞）
            if (!conn.getAutoCommit()) {
                conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            }

            log.debug("从连接池获取SQL Server连接: poolName={}, datasourceId={}", poolName, datasourceId);
            return conn;

        } catch (SQLException e) {
            log.error("获取SQL Server数据库连接失败: poolName={}, datasourceId={}", poolName, datasourceId, e);
            throw EtlException.connectionFailed(databaseName, e);
        }
    }

    @Override
    public void close() {
        // 关闭连接池
        ConnectionPoolManager.closePool(poolName);
        log.info("关闭SQL Server连接池: poolName={}", poolName);
    }

    @Override
    public String getJdbcUrl() {
        StringBuilder url = new StringBuilder();
        url.append(String.format(JDBC_URL_TEMPLATE, host, port, databaseName));
        url.append(";encrypt=false;trustServerCertificate=true");
        url.append(";sendStringParametersAsUnicode=true");
        // 设置 SNAPSHOT 隔离级别支持
        url.append(";useBulkCopyForBatchInsert=true");
        if (extraConfig != null && !extraConfig.isEmpty()) {
            url.append(";").append(extraConfig);
        }
        return url.toString();
    }

    @Override
    protected String getSchemaPattern() {
        // SQL Server 默认使用 dbo schema
        return "dbo";
    }

    @Override
    protected String quoteIdentifier(String identifier) {
        // SQL Server 使用方括号引用标识符
        return "[" + identifier + "]";
    }

    @Override
    public List<String> listDatabases() throws Exception {
        List<String> databases = new ArrayList<>();
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name FROM sys.databases WHERE database_id > 4")) {
            while (rs.next()) {
                databases.add(rs.getString("name"));
            }
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
        try (Connection conn = getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            String[] types = {"TABLE", "VIEW"};

            try (ResultSet rs = metaData.getTables(database, "dbo", "%", types)) {
                while (rs.next()) {
                    TableInfo tableInfo = new TableInfo();
                    tableInfo.setTableName(rs.getString("TABLE_NAME"));
                    tableInfo.setTableComment(rs.getString("REMARKS"));
                    tableInfo.setTableType(rs.getString("TABLE_TYPE"));
                    tableInfo.setSchemaName("dbo");
                    tables.add(tableInfo);
                }
            }
        }
        return tables;
    }

    @Override
    public List<ColumnInfo> getColumns(String tableName) throws Exception {
        List<ColumnInfo> columns = new ArrayList<>();
        try (Connection conn = getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();

            try (ResultSet rs = metaData.getColumns(databaseName, "dbo", tableName, null)) {
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
        }
        return columns;
    }

    @Override
    public String getDatabaseType() {
        return "SQLSERVER";
    }

    /**
     * 获取连接池状态
     */
    public Map<String, Object> getPoolStatus() {
        return ConnectionPoolManager.getPoolStatus(poolName);
    }

    /**
     * 检查 IDENTITY 列
     */
    public boolean isIdentityColumn(String tableName, String columnName) throws Exception {
        String sql = String.format(
            "SELECT COLUMNPROPERTY(OBJECT_ID('%s'), '%s', 'IsIdentity') AS IsIdentity",
            tableName, columnName);
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt("IsIdentity") == 1;
            }
        }
        return false;
    }

    /**
     * 重置 IDENTITY 种子
     */
    public void reseedIdentity(String tableName, long seedValue) throws Exception {
        String sql = String.format("DBCC CHECKIDENT ('%s', RESEED, %d)", tableName, seedValue);
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            log.info("重置 IDENTITY 种子: table={}, seed={}", tableName, seedValue);
        }
    }
}