package com.etl.engine.strategy;

import com.etl.common.domain.SyncPipelineContext;
import com.etl.common.domain.TableInfo;
import com.etl.common.utils.SqlBuilder;
import com.etl.datasource.connector.DatabaseConnector;
import com.etl.datasource.service.DatasourceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 追加同步策略
 * 保留目标表现有数据，支持幂等性（增量写入+upsert）
 */
@Slf4j
@Component
public class AppendStrategy implements SyncStrategy {

    @Autowired
    private DatasourceService datasourceService;

    @Override
    public String getName() {
        return "APPEND";
    }

    @Override
    public long execute(SyncPipelineContext context, List<Map<String, Object>> sourceData) throws Exception {
        log.info("执行追加同步策略, 数据量: {}", sourceData.size());
        if (sourceData == null || sourceData.isEmpty()) {
            return 0;
        }

        String targetTable = context.getTargetTable();
        DatabaseConnector targetConnector = datasourceService.getConnector(context.getTargetDsId());
        String dbType = targetConnector.getDatabaseType();

        // 获取表信息和主键
        TableInfo tableInfo = targetConnector.getTableInfo(targetTable);
        List<String> primaryKeys = tableInfo.getPrimaryKeys();
        String[] columns = tableInfo.getColumns().stream()
                .map(col -> col.getColumnName())
                .toArray(String[]::new);

        // 1. 如果配置了增量字段，过滤数据（只写入增量部分）
        List<Map<String, Object>> filteredData = filterIncrementalData(sourceData, context, targetConnector, targetTable);
        if (filteredData.isEmpty()) {
            log.info("无增量数据需要同步");
            return 0;
        }

        // 2. 决定使用 upsert 还是普通 insert
        long syncCount;
        if (primaryKeys != null && !primaryKeys.isEmpty()) {
            // 有主键，使用 upsert 模式
            syncCount = batchUpsert(filteredData, targetConnector, targetTable, columns, primaryKeys.toArray(new String[0]), dbType);
        } else {
            // 无主键，使用普通 insert
            syncCount = batchInsert(filteredData, targetConnector, targetTable, columns, dbType);
        }

        return syncCount;
    }

    @Override
    public long syncTable(SyncPipelineContext context, String sourceTable, String targetTable) throws Exception {
        log.info("追加同步表: {} -> {}", sourceTable, targetTable);
        // 此方法在其他地方实现，这里保持接口兼容性
        return 0;
    }

    /**
     * 过滤增量数据：只保留大于目标表最大增量值的数据
     */
    private List<Map<String, Object>> filterIncrementalData(List<Map<String, Object>> sourceData,
                                                              SyncPipelineContext context,
                                                              DatabaseConnector targetConnector,
                                                              String targetTable) throws Exception {
        String incrementalField = context.getIncrementalField();
        if (incrementalField == null || incrementalField.isEmpty()) {
            return sourceData;
        }

        // 查询目标表的最大增量值
        Object maxValue = getMaxIncrementalValue(targetConnector, targetTable, incrementalField);
        if (maxValue == null) {
            return sourceData;
        }

        // 过滤数据
        List<Map<String, Object>> filtered = new ArrayList<>();
        for (Map<String, Object> row : sourceData) {
            Object value = row.get(incrementalField);
            if (value != null && compareValue(value, maxValue) > 0) {
                filtered.add(row);
            }
        }
        log.info("增量字段过滤: 字段={}, 目标表最大值={}, 过滤前={}, 过滤后={}",
                incrementalField, maxValue, sourceData.size(), filtered.size());
        return filtered;
    }

    /**
     * 获取目标表的最大增量值
     */
    private Object getMaxIncrementalValue(DatabaseConnector connector, String table, String field) throws Exception {
        String sql = "SELECT MAX(" + quoteIdentifier(field, connector.getDatabaseType()) + ") FROM " + quoteIdentifier(table, connector.getDatabaseType());
        try (Connection conn = connector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getObject(1);
            }
        } catch (SQLException e) {
            // 表不存在等异常，返回 null
            log.warn("查询最大增量值失败: table={}, field={}", table, field, e);
        }
        return null;
    }

    /**
     * 比较两个值
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private int compareValue(Object v1, Object v2) {
        if (v1 instanceof Comparable c1 && v2 instanceof Comparable c2) {
            return c1.compareTo(c2);
        }
        return v1.toString().compareTo(v2.toString());
    }

    /**
     * 批量 upsert 数据
     */
    private long batchUpsert(List<Map<String, Object>> data, DatabaseConnector connector,
                             String table, String[] columns, String[] primaryKeys, String dbType) throws Exception {
        String sql = SqlBuilder.buildUpsertSql(table, columns, primaryKeys, dbType);
        int batchSize = 1000;
        long totalCount = 0;

        for (int i = 0; i < data.size(); i += batchSize) {
            int end = Math.min(i + batchSize, data.size());
            List<Map<String, Object>> batch = data.subList(i, end);

            try (Connection conn = connector.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (Map<String, Object> row : batch) {
                    int idx = 1;
                    for (String col : columns) {
                        stmt.setObject(idx++, row.get(col));
                    }
                    // PostgreSQL upsert 需要额外的 update 部分参数，但 SqlBuilder 已经用了 EXCLUDED，不需要重复
                    // MySQL 不需要额外参数
                    stmt.addBatch();
                }
                int[] counts = stmt.executeBatch();
                for (int c : counts) {
                    totalCount += Math.abs(c);
                }
            }
        }
        log.info("批量 upsert 完成: table={}, count={}", table, totalCount);
        return totalCount;
    }

    /**
     * 批量 insert 数据
     */
    private long batchInsert(List<Map<String, Object>> data, DatabaseConnector connector,
                             String table, String[] columns, String dbType) throws Exception {
        int batchSize = 1000;
        long totalCount = 0;

        for (int i = 0; i < data.size(); i += batchSize) {
            int end = Math.min(i + batchSize, data.size());
            List<Map<String, Object>> batch = data.subList(i, end);

            String sql = SqlBuilder.buildMultiValueInsertSql(table, columns, batch.size(), dbType);

            try (Connection conn = connector.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                int idx = 1;
                for (Map<String, Object> row : batch) {
                    for (String col : columns) {
                        stmt.setObject(idx++, row.get(col));
                    }
                }
                int count = stmt.executeUpdate();
                totalCount += count;
            }
        }
        log.info("批量 insert 完成: table={}, count={}", table, totalCount);
        return totalCount;
    }

    /**
     * 引用标识符
     */
    private String quoteIdentifier(String identifier, String dbType) {
        return SqlBuilder.quoteIdentifier(identifier, dbType);
    }
}
