package com.etl.datasource.connector.bigdata;

import cn.hutool.core.util.StrUtil;
import com.etl.common.domain.ColumnInfo;
import com.etl.common.domain.TableInfo;
import com.etl.common.enums.DataSourceType;
import com.etl.datasource.connector.AbstractConnector;
import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Apache Hive 数据仓库连接器
 */
@Slf4j
public class HiveConnector extends AbstractConnector {

    public HiveConnector(Long datasourceId, String host, Integer port, String databaseName,
                         String username, String password, String charset, String extraConfig) {
        super(datasourceId, host, port != null ? port : 10000, databaseName, username, password, charset, extraConfig);
        this.dataSourceType = DataSourceType.HIVE;
    }

    @Override
    public Connection getConnection() throws Exception {
        if (connection == null || connection.isClosed()) {
            String jdbcUrl = buildJdbcUrl();
            Class.forName(dataSourceType.getDriverClass());

            // 设置连接属性，确保正确的字符编码
            Properties props = new Properties();
            props.setProperty("user", username);
            if (password != null && !password.isEmpty()) {
                props.setProperty("password", password);
            }
            // Hive 字符编码设置
            props.setProperty("charset", StrUtil.isBlank(charset) ? "UTF-8" : charset);

            connection = DriverManager.getConnection(jdbcUrl, props);
            log.info("Hive 连接成功: {}", jdbcUrl);
        }
        return connection;
    }

    @Override
    public String getJdbcUrl() {
        return buildJdbcUrl();
    }

    private String buildJdbcUrl() {
        // Hive JDBC URL: jdbc:hive2://host:port/database
        StringBuilder url = new StringBuilder();
        url.append(String.format("jdbc:hive2://%s:%d/%s", host, port, databaseName));
        if (extraConfig != null && !extraConfig.isEmpty()) {
            url.append(";").append(extraConfig);
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
             ResultSet rs = stmt.executeQuery("SHOW DATABASES")) {
            while (rs.next()) {
                databases.add(rs.getString(1));
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
            String useDb = "USE " + quoteIdentifier(database);
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(useDb);
            }

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SHOW TABLES")) {
                while (rs.next()) {
                    String tableName = rs.getString(1);
                    TableInfo tableInfo = new TableInfo();
                    tableInfo.setTableName(tableName);
                    tableInfo.setSchemaName(database);
                    tableInfo.setTableType("TABLE");
                    tables.add(tableInfo);
                }
            }
        }
        return tables;
    }

    @Override
    public TableInfo getTableInfo(String tableName) throws Exception {
        TableInfo tableInfo = new TableInfo();
        tableInfo.setTableName(tableName);
        tableInfo.setSchemaName(databaseName);
        tableInfo.setTableType("TABLE");
        tableInfo.setColumns(getColumns(tableName));

        // 获取表注释
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("DESCRIBE FORMATTED " + quoteIdentifier(tableName))) {
            while (rs.next()) {
                String col = rs.getString(1);
                if (col != null && col.contains("Comment:")) {
                    tableInfo.setTableComment(rs.getString(2));
                    break;
                }
            }
        } catch (Exception e) {
            log.debug("获取 Hive 表注释失败: {}", e.getMessage());
        }

        return tableInfo;
    }

    @Override
    public List<ColumnInfo> getColumns(String tableName) throws Exception {
        List<ColumnInfo> columns = new ArrayList<>();
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("DESCRIBE " + quoteIdentifier(tableName))) {

            int ordinal = 1;
            while (rs.next()) {
                String colName = rs.getString(1);
                String colType = rs.getString(2);

                // 跳过分区列和表属性
                if (colName == null || colName.startsWith("#") || colName.isEmpty()) {
                    continue;
                }

                ColumnInfo column = new ColumnInfo();
                column.setColumnName(colName.trim());
                column.setColumnType(colType != null ? colType.trim() : "string");
                column.setNullable(true);
                column.setOrdinalPosition(ordinal++);
                columns.add(column);
            }
        }
        return columns;
    }

    @Override
    public List<String> getPrimaryKeys(String tableName) throws Exception {
        // Hive 不支持传统主键
        return new ArrayList<>();
    }

    @Override
    public String getDatabaseType() {
        return "HIVE";
    }
}
