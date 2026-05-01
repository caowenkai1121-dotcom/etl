package com.etl.datasource.connector;

import com.etl.datasource.pool.PoolConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 数据库连接池管理器
 * 使用HikariCP管理ETL数据源的连接池
 * 内置空闲连接池驱逐和后台监控
 */
@Slf4j
public class ConnectionPoolManager {

    private final Map<String, HikariDataSource> pools = new ConcurrentHashMap<>();
    private final ScheduledExecutorService evictionScheduler;

    private static volatile ConnectionPoolManager INSTANCE;
    /** 允许测试时注入自定义实例 */
    static volatile ConnectionPoolManager testInstance;

    private static final long IDLE_EVICTION_MINUTES = 30;
    private static final long EVICTION_CHECK_INTERVAL_MINUTES = 5;

    private ConnectionPoolManager() {
        this.evictionScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "cp-evict");
            t.setDaemon(true);
            return t;
        });
        this.evictionScheduler.scheduleWithFixedDelay(
            this::evictIdlePools,
            EVICTION_CHECK_INTERVAL_MINUTES,
            EVICTION_CHECK_INTERVAL_MINUTES,
            TimeUnit.MINUTES);
        log.info("连接池管理器初始化完成，驱逐间隔={}分钟，空闲超时={}分钟",
            EVICTION_CHECK_INTERVAL_MINUTES, IDLE_EVICTION_MINUTES);
    }

    private static ConnectionPoolManager getInstance() {
        if (testInstance != null) return testInstance;
        if (INSTANCE == null) {
            synchronized (ConnectionPoolManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ConnectionPoolManager();
                }
            }
        }
        return INSTANCE;
    }

    /** 驱逐超过30分钟无活动的连接池 */
    private void evictIdlePools() {
        long now = System.currentTimeMillis();
        long idleThreshold = IDLE_EVICTION_MINUTES * 60 * 1000;
        pools.forEach((name, pool) -> {
            if (pool.isClosed()) {
                pools.remove(name);
                return;
            }
            var mxBean = pool.getHikariPoolMXBean();
            if (mxBean != null && mxBean.getActiveConnections() == 0) {
                pools.remove(name);
                pool.close();
                log.info("驱逐空闲连接池: poolName={}", name);
            }
        });
    }

    /**
     * 获取或创建连接池（使用默认配置）
     */
    public static HikariDataSource getOrCreatePool(String poolName, String jdbcUrl,
                                                    String username, String password,
                                                    String driverClass) {
        return getOrCreatePool(poolName, jdbcUrl, username, password, driverClass, PoolConfig.builder().build());
    }

    /**
     * 获取或创建连接池（使用自定义配置）
     */
    public static HikariDataSource getOrCreatePool(String poolName, String jdbcUrl,
                                                    String username, String password,
                                                    String driverClass, PoolConfig poolConfig) {
        return getInstance().pools.computeIfAbsent(poolName, key -> {
            log.info("创建连接池: poolName={}, jdbcUrl={}", poolName, jdbcUrl);

            HikariConfig config = new HikariConfig();
            config.setPoolName(poolName);
            config.setJdbcUrl(jdbcUrl);
            config.setUsername(username);
            config.setPassword(password);
            config.setDriverClassName(driverClass);

            // 连接池大小（默认值保护）
            int maxPoolSize = poolConfig.getMaximumPoolSize() > 0 ? poolConfig.getMaximumPoolSize() : 10;
            int minIdle = Math.max(1, poolConfig.getMinimumIdle());
            config.setMaximumPoolSize(maxPoolSize);
            config.setMinimumIdle(minIdle);

            // 超时配置
            config.setConnectionTimeout(poolConfig.getConnectionTimeoutMs());
            config.setIdleTimeout(Math.min(poolConfig.getIdleTimeoutMs(), 600_000));
            config.setMaxLifetime(Math.min(poolConfig.getMaxLifetimeMs(), 1_800_000));
            config.setLeakDetectionThreshold(poolConfig.getLeakDetectionThresholdMs());
            config.setValidationTimeout(poolConfig.getValidationTimeoutMs());

            // 连接测试
            config.setConnectionTestQuery("SELECT 1");

            // 性能优化
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.addDataSourceProperty("useServerPrepStmts", "true");
            config.addDataSourceProperty("useLocalSessionState", "true");
            config.addDataSourceProperty("rewriteBatchedStatements", "true");
            config.addDataSourceProperty("cacheResultSetMetadata", "true");
            config.addDataSourceProperty("cacheServerConfiguration", "true");
            config.addDataSourceProperty("elideSetAutoCommits", "true");
            config.addDataSourceProperty("maintainTimeStats", "false");

            HikariDataSource ds = new HikariDataSource(config);
            log.info("连接池创建成功: poolName={}, maxSize={}, minIdle={}", poolName, maxPoolSize, minIdle);
            return ds;
        });
    }

    /**
     * 获取连接
     */
    public static Connection getConnection(String poolName) throws SQLException {
        ConnectionPoolManager mgr = getInstance();
        HikariDataSource pool = mgr.pools.get(poolName);
        if (pool == null) {
            throw new SQLException("连接池不存在: " + poolName);
        }
        if (pool.isClosed()) {
            throw new SQLException("连接池已关闭: " + poolName);
        }

        var mxBean = pool.getHikariPoolMXBean();
        if (mxBean != null && mxBean.getThreadsAwaitingConnection() > 0) {
            log.warn("连接池等待中: poolName={}, active={}, idle={}, waiting={}",
                poolName, mxBean.getActiveConnections(), mxBean.getIdleConnections(),
                mxBean.getThreadsAwaitingConnection());
        }

        return pool.getConnection();
    }

    /**
     * 获取连接，如果连接池不存在则自动创建（线程安全）
     * 此方法解决了多实例场景下的竞态条件问题
     */
    public static Connection getOrCreateConnection(String poolName, String jdbcUrl,
                                                    String username, String password,
                                                    String driverClass) throws SQLException {
        return getOrCreateConnection(poolName, jdbcUrl, username, password, driverClass, PoolConfig.builder().build());
    }

    /**
     * 获取连接，如果连接池不存在则自动创建（线程安全，自定义配置）
     */
    public static Connection getOrCreateConnection(String poolName, String jdbcUrl,
                                                    String username, String password,
                                                    String driverClass, PoolConfig poolConfig) throws SQLException {
        // 先尝试直接获取连接池
        ConnectionPoolManager mgr = getInstance();
        HikariDataSource pool = mgr.pools.get(poolName);

        if (pool != null && !pool.isClosed()) {
            return pool.getConnection();
        }

        // 连接池不存在或已关闭，创建新连接池
        // computeIfAbsent 保证线程安全，同一 poolName 只会创建一次
        pool = getOrCreatePool(poolName, jdbcUrl, username, password, driverClass, poolConfig);

        if (pool.isClosed()) {
            throw new SQLException("连接池已关闭: " + poolName);
        }

        return pool.getConnection();
    }

    /**
     * 关闭连接池
     */
    public static void closePool(String poolName) {
        ConnectionPoolManager mgr = getInstance();
        HikariDataSource pool = mgr.pools.remove(poolName);
        if (pool != null && !pool.isClosed()) {
            log.info("关闭连接池: poolName={}", poolName);
            pool.close();
        }
    }

    /**
     * 关闭所有连接池
     */
    public static void closeAll() {
        ConnectionPoolManager mgr = getInstance();
        log.info("关闭所有连接池, 数量: {}", mgr.pools.size());
        mgr.pools.forEach((name, pool) -> {
            if (!pool.isClosed()) {
                pool.close();
            }
        });
        mgr.pools.clear();
        mgr.evictionScheduler.shutdown();
        try {
            if (!mgr.evictionScheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                mgr.evictionScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            mgr.evictionScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("所有连接池已关闭");
    }

    /**
     * 获取连接池状态
     */
    public static Map<String, Object> getPoolStatus(String poolName) {
        ConnectionPoolManager mgr = getInstance();
        HikariDataSource pool = mgr.pools.get(poolName);
        if (pool == null) {
            return null;
        }

        Map<String, Object> status = new java.util.LinkedHashMap<>();
        status.put("poolName", poolName);
        status.put("isClosed", pool.isClosed());

        var mxBean = pool.getHikariPoolMXBean();
        if (mxBean != null) {
            status.put("activeConnections", mxBean.getActiveConnections());
            status.put("idleConnections", mxBean.getIdleConnections());
            status.put("totalConnections", mxBean.getTotalConnections());
            status.put("threadsAwaitingConnection", mxBean.getThreadsAwaitingConnection());
        } else {
            status.put("activeConnections", -1);
            status.put("idleConnections", -1);
            status.put("totalConnections", -1);
            status.put("threadsAwaitingConnection", -1);
        }
        return status;
    }

    /**
     * 获取所有连接池状态
     */
    public static Map<String, Map<String, Object>> getAllPoolStatus() {
        Map<String, Map<String, Object>> result = new java.util.LinkedHashMap<>();
        getInstance().pools.keySet().forEach(name -> {
            result.put(name, getPoolStatus(name));
        });
        return result;
    }

    /** 返回连接池数量（监控用） */
    public static int getPoolCount() {
        return getInstance().pools.size();
    }
}
