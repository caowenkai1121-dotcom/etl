package com.etl.datasource.connector;

import com.etl.common.enums.DataSourceType;
import com.etl.common.exception.EtlException;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Apache Doris连接器 - 使用HikariCP连接池
 * Doris使用MySQL协议，兼容MySQL驱动
 */
@Slf4j
public class DorisConnector extends AbstractConnector {

    private static final String JDBC_URL_TEMPLATE = "jdbc:mysql://%s:%d/%s?useUnicode=true&characterEncoding=%s&useSSL=false";

    // 连接池名称
    private final String poolName;

    public DorisConnector(Long datasourceId, String host, Integer port, String databaseName,
                          String username, String password, String charset, String extraConfig) {
        super(datasourceId, host, port, databaseName, username, password, charset, extraConfig);
        this.dataSourceType = DataSourceType.DORIS;
        this.poolName = "doris-pool-" + datasourceId;
    }

    @Override
    public Connection getConnection() throws Exception {
        try {
            String url = getJdbcUrl();
            // 使用线程安全的方法获取连接（自动创建连接池）
            // Doris兼容MySQL协议，使用MySQL驱动
            Connection conn = ConnectionPoolManager.getOrCreateConnection(
                poolName, url, username, password, DataSourceType.MYSQL.getDriverClass());
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
        log.info("关闭Doris连接池: poolName={}", poolName);
    }

    @Override
    public String getJdbcUrl() {
        String url = String.format(JDBC_URL_TEMPLATE, host, port, databaseName, charset);
        if (extraConfig != null && !extraConfig.isEmpty()) {
            url += "&" + extraConfig;
        }
        return url;
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
    public String getDatabaseType() {
        return "DORIS";
    }

    /**
     * 获取连接池状态
     */
    public java.util.Map<String, Object> getPoolStatus() {
        return ConnectionPoolManager.getPoolStatus(poolName);
    }
}
