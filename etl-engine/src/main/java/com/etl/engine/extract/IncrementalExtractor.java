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
import java.util.concurrent.atomic.AtomicReference;

/**
 * 增量数据抽取器
 */
@Slf4j
public class IncrementalExtractor implements DataExtractor {

    private final DatabaseConnector connector;
    private final String tableName;
    private final String incrementalField;
    private final Object lastValue;
    private final int batchSize;
    private final AtomicBoolean stopped;
    private final AtomicReference<Object> maxIncrementalValue;

    public IncrementalExtractor(DatabaseConnector connector, String tableName, String incrementalField,
                                Object lastValue, int batchSize) {
        this(connector, tableName, incrementalField, lastValue, batchSize, new AtomicBoolean(false));
    }

    public IncrementalExtractor(DatabaseConnector connector, String tableName, String incrementalField,
                                Object lastValue, int batchSize, AtomicBoolean stopped) {
        this.connector = connector;
        this.tableName = tableName;
        this.incrementalField = incrementalField;
        this.lastValue = lastValue;
        this.batchSize = batchSize;
        this.stopped = stopped;
        this.maxIncrementalValue = new AtomicReference<>(lastValue);
    }

    @Override
    public void extract(SyncPipelineContext context, DataHandler handler) throws Exception {
        String dbType = connector.getDatabaseType().toLowerCase();

        // 构建增量查询SQL
        String incrementalSql = buildIncrementalQuerySql(tableName, incrementalField, dbType);
        log.debug("增量查询SQL: {}", incrementalSql);

        try (Connection conn = connector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(incrementalSql)) {

            // 设置增量字段参数
            stmt.setObject(1, lastValue);

            try (ResultSet rs = stmt.executeQuery()) {
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();
                List<Map<String, Object>> batch = new ArrayList<>();

                while (rs.next() && !stopped.get()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = metaData.getColumnName(i);
                        Object value = rs.getObject(i);
                        row.put(columnName, value);
                    }
                    batch.add(row);

                    // 更新最大增量值
                    Object currentValue = row.get(incrementalField);
                    updateMaxIncrementalValue(currentValue);

                    if (batch.size() >= batchSize) {
                        handler.handle(batch);
                        batch = new ArrayList<>();
                        log.debug("已抽取 {} 条增量数据，当前最大增量值: {}", batchSize, maxIncrementalValue.get());
                    }
                }

                // 处理剩余数据
                if (!batch.isEmpty()) {
                    handler.handle(batch);
                    log.debug("已抽取剩余 {} 条增量数据", batch.size());
                }
            }
        }

        log.info("增量抽取完成: {}, 最大增量值: {}", tableName, maxIncrementalValue.get());
    }

    /**
     * 构建增量查询SQL
     */
    private String buildIncrementalQuerySql(String table, String field, String dbType) {
        String quotedTable = quoteIdentifier(table, dbType);
        String quotedField = quoteIdentifier(field, dbType);
        return "SELECT * FROM " + quotedTable + " WHERE " + quotedField + " >= ?";
    }

    /**
     * 引用标识符
     */
    private String quoteIdentifier(String identifier, String dbType) {
        if (identifier == null || identifier.isEmpty()) {
            return "";
        }
        if ("postgresql".equalsIgnoreCase(dbType)) {
            return "\"" + identifier.replace("\"", "\"\"") + "\"";
        }
        return "`" + identifier.replace("`", "``") + "`";
    }

    /**
     * 更新最大增量值
     */
    private void updateMaxIncrementalValue(Object value) {
        if (value == null) {
            return;
        }

        Object currentMax = maxIncrementalValue.get();
        if (currentMax == null) {
            maxIncrementalValue.set(value);
            return;
        }

        // 比较增量值
        if (value instanceof Comparable) {
            @SuppressWarnings("unchecked")
            Comparable<Object> comparableValue = (Comparable<Object>) value;
            if (comparableValue.compareTo(currentMax) > 0) {
                maxIncrementalValue.set(value);
            }
        }
    }

    /**
     * 获取最大增量值
     */
    public Object getMaxIncrementalValue() {
        return maxIncrementalValue.get();
    }
}
