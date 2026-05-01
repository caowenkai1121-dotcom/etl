package com.etl.datasource.connector;

import com.etl.common.enums.DataSourceType;
import com.etl.common.exception.EtlException;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * PostgreSQL连接器 - 使用HikariCP连接池
 */
@Slf4j
public class PostgreSQLConnector extends AbstractConnector {

    private static final String JDBC_URL_TEMPLATE = "jdbc:postgresql://%s:%d/%s";

    // 连接池名称
    private final String poolName;

    public PostgreSQLConnector(Long datasourceId, String host, Integer port, String databaseName,
                               String username, String password, String charset, String extraConfig) {
        super(datasourceId, host, port, databaseName, username, password, charset, extraConfig);
        this.dataSourceType = DataSourceType.POSTGRESQL;
        this.poolName = "postgresql-pool-" + datasourceId;
    }

    @Override
    public Connection getConnection() throws Exception {
        try {
            String url = getJdbcUrl();
            // 使用线程安全的方法获取连接（自动创建连接池）
            Connection conn = ConnectionPoolManager.getOrCreateConnection(
                poolName, url, username, password, dataSourceType.getDriverClass());
            log.debug("从连接池获取连接: poolName={}, datasourceId={}", poolName, datasourceId);
            return conn;

        } catch (SQLException e) {
            log.error("获取数据库连接失败: poolName={}, datasourceId={}", poolName, datasourceId, e);
            throw EtlException.connectionFailed(databaseName, e);
        }
    }

    @Override
    public void close() {
        // 关闭连接池
        ConnectionPoolManager.closePool(poolName);
        log.info("关闭PostgreSQL连接池: poolName={}", poolName);
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
        return "public";
    }

    @Override
    protected String quoteIdentifier(String identifier) {
        return "\"" + identifier + "\"";
    }

    @Override
    public String getDatabaseType() {
        return "POSTGRESQL";
    }

    /**
     * 获取连接池状态
     */
    public java.util.Map<String, Object> getPoolStatus() {
        return ConnectionPoolManager.getPoolStatus(poolName);
    }
}
