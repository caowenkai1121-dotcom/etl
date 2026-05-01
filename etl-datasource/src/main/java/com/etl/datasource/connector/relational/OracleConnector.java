package com.etl.datasource.connector.relational;

import cn.hutool.core.util.StrUtil;
import com.etl.common.domain.ColumnInfo;
import com.etl.common.domain.TableInfo;
import com.etl.common.enums.DataSourceType;
import com.etl.common.exception.EtlException;
import com.etl.datasource.connector.AbstractConnector;
import com.etl.datasource.connector.ConnectionPoolManager;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.io.Reader;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Oracle 数据库连接器
 * 使用 HikariCP 连接池管理连接
 */
@Slf4j
public class OracleConnector extends AbstractConnector {

    private static final String JDBC_URL_TEMPLATE = "jdbc:oracle:thin:@//%s:%d/%s";

    // 连接池名称
    private final String poolName;

    // NLS 设置
    private static final String NLS_DATE_FORMAT = "YYYY-MM-DD HH24:MI:SS";
    private static final String NLS_TIMESTAMP_FORMAT = "YYYY-MM-DD HH24:MI:SS.FF6";
    private static final String NLS_TIMESTAMP_TZ_FORMAT = "YYYY-MM-DD HH24:MI:SS.FF6 TZR";

    public OracleConnector(Long datasourceId, String host, Integer port, String databaseName,
                           String username, String password, String charset, String extraConfig) {
        super(datasourceId, host, port, databaseName, username, password, charset, extraConfig);
        this.dataSourceType = DataSourceType.ORACLE;
        this.poolName = "oracle-pool-" + datasourceId;
    }

    @Override
    public Connection getConnection() throws Exception {
        try {
            String url = getJdbcUrl();
            // 使用线程安全的方法获取连接（自动创建连接池）
            Connection conn = ConnectionPoolManager.getOrCreateConnection(
                poolName, url, username, password, dataSourceType.getDriverClass());

            // 设置 NLS 会话参数
            configureNlsSession(conn);

            log.debug("从连接池获取Oracle连接: poolName={}, datasourceId={}", poolName, datasourceId);
            return conn;

        } catch (SQLException e) {
            log.error("获取Oracle数据库连接失败: poolName={}, datasourceId={}", poolName, datasourceId, e);
            throw EtlException.connectionFailed(databaseName, e);
        }
    }

    /**
     * 配置 NLS 会话参数
     * 解决日期格式、字符编码等问题
     */
    private void configureNlsSession(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // 设置日期时间格式
            stmt.execute("ALTER SESSION SET NLS_DATE_FORMAT = '" + NLS_DATE_FORMAT + "'");
            stmt.execute("ALTER SESSION SET NLS_TIMESTAMP_FORMAT = '" + NLS_TIMESTAMP_FORMAT + "'");
            stmt.execute("ALTER SESSION SET NLS_TIMESTAMP_TZ_FORMAT = '" + NLS_TIMESTAMP_TZ_FORMAT + "'");

            // 设置小数分隔符和千位分隔符
            stmt.execute("ALTER SESSION SET NLS_NUMERIC_CHARACTERS = '.,'");

            // 设置字符集
            if (!StrUtil.isBlank(charset)) {
                stmt.execute("ALTER SESSION SET NLS_CHARACTERSET = '" + charset + "'");
            }

            log.debug("Oracle NLS 会话配置完成");
        } catch (SQLException e) {
            log.warn("设置Oracle NLS参数失败（非致命错误）: {}", e.getMessage());
        }
    }

    @Override
    public void close() {
        // 关闭连接池
        ConnectionPoolManager.closePool(poolName);
        log.info("关闭Oracle连接池: poolName={}", poolName);
    }

    @Override
    public String getJdbcUrl() {
        String url = String.format(JDBC_URL_TEMPLATE, host, port, databaseName);
        if (extraConfig != null && !extraConfig.isEmpty()) {
            url += "?" + extraConfig;
        }
        return url;
    }

    @Override
    protected String getSchemaPattern() {
        // Oracle 使用用户名（大写）作为 Schema
        if (username == null || username.isEmpty()) {
            return databaseName != null ? databaseName.toUpperCase() : null;
        }
        return username.toUpperCase();
    }

    @Override
    protected String quoteIdentifier(String identifier) {
        // Oracle 使用双引号引用标识符
        return "\"" + identifier + "\"";
    }

    @Override
    public List<String> listDatabases() throws Exception {
        List<String> databases = new ArrayList<>();
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT NAME FROM V$DATABASE")) {
            while (rs.next()) {
                databases.add(rs.getString("NAME"));
            }
        }
        return databases;
    }

    @Override
    public List<TableInfo> getTables() throws Exception {
        return getTables(getSchemaPattern());
    }

    @Override
    public List<TableInfo> getTables(String schema) throws Exception {
        List<TableInfo> tables = new ArrayList<>();
        try (Connection conn = getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            String schemaPattern = schema != null ? schema.toUpperCase() : null;
            try (ResultSet rs = metaData.getTables(null, schemaPattern, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    TableInfo tableInfo = new TableInfo();
                    tableInfo.setTableName(rs.getString("TABLE_NAME"));
                    tableInfo.setTableComment(rs.getString("REMARKS"));
                    tableInfo.setTableType(rs.getString("TABLE_TYPE"));
                    tableInfo.setSchemaName(schemaPattern);
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
            String schema = getSchemaPattern();

            try (ResultSet rs = metaData.getColumns(null, schema, tableName, null)) {
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
        return "ORACLE";
    }

    /**
     * 获取连接池状态
     */
    public Map<String, Object> getPoolStatus() {
        return ConnectionPoolManager.getPoolStatus(poolName);
    }

    /**
     * 读取 CLOB 字段内容
     * 使用流式读取避免截断问题
     */
    public String readClob(Clob clob) throws Exception {
        if (clob == null) {
            return null;
        }
        try (Reader reader = clob.getCharacterStream()) {
            StringBuilder sb = new StringBuilder();
            char[] buffer = new char[4096];
            int len;
            while ((len = reader.read(buffer)) != -1) {
                sb.append(buffer, 0, len);
            }
            return sb.toString();
        }
    }

    /**
     * 读取 BLOB 字段内容
     */
    public byte[] readBlob(Blob blob) throws Exception {
        if (blob == null) {
            return null;
        }
        try (InputStream is = blob.getBinaryStream()) {
            return is.readAllBytes();
        }
    }
}
