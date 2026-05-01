package com.etl.datasource.connector.bigdata;

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
 * ClickHouse 数据库连接器
 * 高性能列式存储数据库，使用 HikariCP 连接池
 */
@Slf4j
public class ClickHouseConnector extends AbstractConnector {

    private static final String JDBC_URL_TEMPLATE = "jdbc:clickhouse://%s:%d/%s";

    // 连接池名称
    private final String poolName;

    public ClickHouseConnector(Long datasourceId, String host, Integer port, String databaseName,
                               String username, String password, String charset, String extraConfig) {
        super(datasourceId, host, port != null ? port : 8123, databaseName, username, password, charset, extraConfig);
        this.dataSourceType = DataSourceType.CLICKHOUSE;
        this.poolName = "clickhouse-pool-" + datasourceId;
    }

    @Override
    public Connection getConnection() throws Exception {
        try {
            String url = getJdbcUrl();
            // 使用线程安全的方法获取连接（自动创建连接池）
            Connection conn = ConnectionPoolManager.getOrCreateConnection(
                poolName, url, username, password, dataSourceType.getDriverClass());
            log.debug("从连接池获取ClickHouse连接: poolName={}, datasourceId={}", poolName, datasourceId);
            return conn;

        } catch (SQLException e) {
            log.error("获取ClickHouse数据库连接失败: poolName={}, datasourceId={}", poolName, datasourceId, e);
            throw EtlException.connectionFailed(databaseName, e);
        }
    }

    @Override
    public void close() {
        // 关闭连接池
        ConnectionPoolManager.closePool(poolName);
        log.info("关闭ClickHouse连接池: poolName={}", poolName);
    }

    @Override
    public String getJdbcUrl() {
        StringBuilder url = new StringBuilder();
        url.append(String.format(JDBC_URL_TEMPLATE, host, port, databaseName));
        if (!StrUtil.isBlank(charset)) {
            url.append("?charset=").append(charset);
        }
        if (extraConfig != null && !extraConfig.isEmpty()) {
            url.append(url.toString().contains("?") ? "&" : "?").append(extraConfig);
        }
        return url.toString();
    }

    @Override
    protected String getSchemaPattern() {
        return databaseName;
    }

    @Override
    protected String quoteIdentifier(String identifier) {
        return "`" + identifier + "`";
    }

    @Override
    public List<String> listDatabases() throws Exception {
        List<String> databases = new ArrayList<>();
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name FROM system.databases")) {
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
        String sql = "SELECT database, name, engine FROM system.tables WHERE database = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, database);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    TableInfo tableInfo = new TableInfo();
                    tableInfo.setTableName(rs.getString("name"));
                    tableInfo.setSchemaName(rs.getString("database"));
                    tableInfo.setTableType(rs.getString("engine"));
                    tables.add(tableInfo);
                }
            }
        }
        return tables;
    }

    @Override
    public List<ColumnInfo> getColumns(String tableName) throws Exception {
        List<ColumnInfo> columns = new ArrayList<>();
        String sql = "SELECT name, type, default_kind, comment FROM system.columns WHERE database = ? AND table = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, databaseName);
            stmt.setString(2, tableName);
            try (ResultSet rs = stmt.executeQuery()) {
                int ordinal = 1;
                while (rs.next()) {
                    ColumnInfo column = new ColumnInfo();
                    column.setColumnName(rs.getString("name"));
                    column.setColumnType(rs.getString("type"));
                    column.setDefaultValue(rs.getString("default_kind"));
                    column.setColumnComment(rs.getString("comment"));
                    column.setNullable(true);
                    column.setOrdinalPosition(ordinal++);
                    columns.add(column);
                }
            }
        }
        return columns;
    }

    @Override
    public List<String> getPrimaryKeys(String tableName) throws Exception {
        // ClickHouse 不支持传统主键，返回排序键
        List<String> keys = new ArrayList<>();
        String sql = "SELECT sorting_key FROM system.tables WHERE database = ? AND name = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, databaseName);
            stmt.setString(2, tableName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String sortingKey = rs.getString("sorting_key");
                    if (sortingKey != null && !sortingKey.isEmpty()) {
                        keys.addAll(List.of(sortingKey.split(",")));
                    }
                }
            }
        }
        return keys;
    }

    @Override
    public String getDatabaseType() {
        return "CLICKHOUSE";
    }

    /**
     * 获取连接池状态
     */
    public Map<String, Object> getPoolStatus() {
        return ConnectionPoolManager.getPoolStatus(poolName);
    }

    /**
     * ClickHouse 不支持 CDC 作为源
     */
    @Override
    public boolean supportsCdc() {
        return false;
    }
}
