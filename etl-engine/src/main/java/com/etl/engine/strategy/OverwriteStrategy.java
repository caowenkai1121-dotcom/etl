package com.etl.engine.strategy;

import com.etl.common.domain.SyncPipelineContext;
import com.etl.common.domain.TableInfo;
import com.etl.common.utils.SqlBuilder;
import com.etl.datasource.connector.DatabaseConnector;
import com.etl.datasource.service.DatasourceService;
import com.etl.engine.rollback.RollbackManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

/**
 * 覆盖同步策略
 * 使用临时表方案实现失败回滚，原子替换目标表
 */
@Slf4j
@Component
public class OverwriteStrategy implements SyncStrategy {

    @Autowired
    private DatasourceService datasourceService;

    @Autowired
    private RollbackManager rollbackManager;

    @Override
    public String getName() {
        return "OVERWRITE";
    }

    @Override
    public long execute(SyncPipelineContext context, List<Map<String, Object>> sourceData) throws Exception {
        log.info("执行覆盖同步策略, 数据量: {}", sourceData.size());
        if (sourceData == null || sourceData.isEmpty()) {
            return 0;
        }

        String targetTable = context.getTargetTable();
        DatabaseConnector targetConnector = datasourceService.getConnector(context.getTargetDsId());

        // 检查目标表是否存在
        boolean tableExists = checkTableExists(targetConnector, targetTable);

        if (!tableExists) {
            // 目标表不存在，直接创建并写入
            log.info("目标表不存在，直接创建并写入: {}", targetTable);
            return createTableAndInsert(sourceData, targetConnector, targetTable, context);
        }

        // 目标表存在，使用临时表方案
        try (RollbackManager.RollbackSession session = rollbackManager.beginRollbackSession(targetConnector, targetTable)) {
            // 1. 准备临时表
            session.prepare();

            // 2. 写入数据到临时表
            String tmpTable = session.getTmpTable();
            long syncCount = insertDataToTable(sourceData, targetConnector, tmpTable, context);

            // 3. 提交：临时表替换目标表
            session.commit();

            log.info("覆盖同步完成: 原表={}, 临时表={}, 同步行数={}", targetTable, tmpTable, syncCount);
            return syncCount;
        } catch (Exception e) {
            log.error("覆盖同步失败，已回滚", e);
            throw e;
        }
    }

    @Override
    public long syncTable(SyncPipelineContext context, String sourceTable, String targetTable) throws Exception {
        log.info("覆盖同步表: {} -> {}", sourceTable, targetTable);
        // 此方法在其他地方实现，这里保持接口兼容性
        return 0;
    }

    /**
     * 检查表是否存在
     */
    private boolean checkTableExists(DatabaseConnector connector, String table) {
        try {
            TableInfo tableInfo = connector.getTableInfo(table);
            return tableInfo != null && tableInfo.getTableName() != null;
        } catch (Exception e) {
            log.warn("检查表是否存在失败: table={}", table, e);
            return false;
        }
    }

    /**
     * 创建表并插入数据（首次同步）
     */
    private long createTableAndInsert(List<Map<String, Object>> sourceData,
                                       DatabaseConnector connector,
                                       String table,
                                       SyncPipelineContext context) throws Exception {
        // 此方法需要根据源表结构创建目标表
        // 这里简化处理，只实现插入逻辑
        return insertDataToTable(sourceData, connector, table, context);
    }

    /**
     * 插入数据到指定表
     */
    private long insertDataToTable(List<Map<String, Object>> sourceData,
                                    DatabaseConnector connector,
                                    String table,
                                    SyncPipelineContext context) throws Exception {
        TableInfo tableInfo;
        try {
            tableInfo = connector.getTableInfo(table);
        } catch (Exception e) {
            log.warn("获取表信息失败，将使用数据中的字段: table={}", table, e);
            // 从数据中推断列名
            if (sourceData.isEmpty()) {
                return 0;
            }
            String[] columns = sourceData.get(0).keySet().toArray(new String[0]);
            return batchInsert(sourceData, connector, table, columns, connector.getDatabaseType());
        }

        String[] columns = tableInfo.getColumns().stream()
                .map(col -> col.getColumnName())
                .toArray(String[]::new);

        return batchInsert(sourceData, connector, table, columns, connector.getDatabaseType());
    }

    /**
     * 批量插入数据
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
        log.info("批量插入完成: table={}, count={}", table, totalCount);
        return totalCount;
    }
}
