package com.etl.engine.extract;

import com.etl.common.domain.SyncPipelineContext;
import com.etl.datasource.connector.DatabaseConnector;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 全量表数据抽取器
 * 使用游标分页方式流式读取源表数据
 */
@Slf4j
public class FullTableExtractor implements DataExtractor {

    private final DatabaseConnector connector;
    private final String tableName;
    private final int batchSize;
    private final String primaryKey;
    private final AtomicBoolean stopped;

    public FullTableExtractor(DatabaseConnector connector, String tableName, int batchSize, String primaryKey) {
        this(connector, tableName, batchSize, primaryKey, new AtomicBoolean(false));
    }

    public FullTableExtractor(DatabaseConnector connector, String tableName, int batchSize, String primaryKey, AtomicBoolean stopped) {
        this.connector = connector;
        this.tableName = tableName;
        this.batchSize = batchSize;
        this.primaryKey = primaryKey;
        this.stopped = stopped;
    }

    @Override
    public void extract(SyncPipelineContext context, DataHandler handler) throws Exception {
        String dbType = connector.getDatabaseType().toLowerCase();

        // 1. 获取近似总行数
        long approximateCount = getApproximateRowCount(dbType);
        context.setTotalRows(approximateCount);
        log.info("表 {} 近似行数: {}", tableName, approximateCount);

        // 2. 使用游标分页方式抽取数据
        long lastMaxId = 0;
        boolean hasMore = true;

        try (Connection conn = connector.getConnection()) {
            while (hasMore && !stopped.get()) {
                List<Map<String, Object>> batch = new ArrayList<>();
                long currentMaxId = lastMaxId;

                // 构建分页查询SQL
                String sql = buildBatchExtractSql(tableName, primaryKey, lastMaxId, batchSize, dbType);

                try (PreparedStatement stmt = conn.prepareStatement(sql);
                     ResultSet rs = stmt.executeQuery()) {

                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();

                    while (rs.next() && !stopped.get()) {
                        Map<String, Object> row = new HashMap<>();
                        for (int i = 1; i <= columnCount; i++) {
                            String columnName = metaData.getColumnName(i);
                            Object value = rs.getObject(i);
                            row.put(columnName, value);
                        }
                        batch.add(row);

                        // 更新当前批次的最大ID
                        Object pkValue = rs.getObject(primaryKey);
                        if (pkValue instanceof Number) {
                            currentMaxId = Math.max(currentMaxId, ((Number) pkValue).longValue());
                        }
                    }
                }

                // 处理数据
                if (!batch.isEmpty()) {
                    handler.handle(batch);
                    lastMaxId = currentMaxId;
                    log.debug("已抽取 {} 条数据，当前最大ID: {}", batch.size(), lastMaxId);
                }

                // 判断是否还有更多数据
                hasMore = batch.size() == batchSize;
            }
        }

        log.info("全表抽取完成: {}", tableName);
    }

    /**
     * 构建分页查询SQL
     */
    private String buildBatchExtractSql(String table, String primaryKey, long lastId, int batchSize, String dbType) {
        // 校验标识符，防止SQL注入
        if (!isValidIdentifier(table) || !isValidIdentifier(primaryKey)) {
            throw new IllegalArgumentException("非法的表名或主键列名");
        }
        String quotedTable = quoteIdentifier(table, dbType);
        String quotedPk = quoteIdentifier(primaryKey, dbType);
        return "SELECT * FROM " + quotedTable +
               " WHERE " + quotedPk + " > " + lastId +
               " ORDER BY " + quotedPk +
               " LIMIT " + batchSize;
    }

    /**
     * 校验标识符合法性，只允许字母、数字、下划线
     */
    private boolean isValidIdentifier(String identifier) {
        if (identifier == null || identifier.isEmpty()) return false;
        return identifier.matches("[a-zA-Z_][a-zA-Z0-9_]*");
    }

    /**
     * 引用标识符
     */
    private String quoteIdentifier(String identifier, String dbType) {
        if ("postgresql".equalsIgnoreCase(dbType)) {
            return "\"" + identifier.replace("\"", "\"\"") + "\"";
        }
        return "`" + identifier.replace("`", "``") + "`";
    }

    /**
     * 获取近似行数
     */
    private long getApproximateRowCount(String dbType) throws Exception {
        String sql;
        if ("postgresql".equalsIgnoreCase(dbType)) {
            sql = "SELECT reltuples AS approximate_count FROM pg_class WHERE relname = ?";
        } else {
            sql = "SELECT TABLE_ROWS AS approximate_count FROM information_schema.TABLES WHERE TABLE_NAME = ?";
        }

        try (Connection conn = connector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, tableName);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getLong(1);
            }
        }

        // 如果近似查询失败，使用精确COUNT
        log.warn("近似行数查询失败，使用精确COUNT");
        return connector.getRowCount(tableName);
    }
}
