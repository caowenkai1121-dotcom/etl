package com.etl.engine.incremental;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.etl.common.domain.ColumnInfo;
import com.etl.common.domain.SyncPipelineContext;
import com.etl.common.domain.TableInfo;
import com.etl.common.enums.ExecutionStatus;
import com.etl.common.enums.SyncMode;
import com.etl.common.enums.TaskStatus;
import com.etl.common.enums.TriggerType;
import com.etl.common.exception.EtlException;
import com.etl.common.utils.JsonUtil;
import com.etl.datasource.connector.DatabaseConnector;
import com.etl.datasource.service.DatasourceService;
import com.etl.engine.SyncEngine;
import com.etl.engine.entity.EtlSyncTask;
import com.etl.engine.entity.EtlTaskExecution;
import com.etl.engine.service.SyncTaskService;
import com.etl.engine.service.TaskExecutionService;
import com.etl.engine.schema.TableSchemaService;
import com.etl.engine.schema.TypeMappingService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 增量同步引擎
 * 支持基于时间戳或自增ID的增量同步
 */
@Slf4j
public class IncrementalSyncEngine implements SyncEngine {

    private final SyncTaskService syncTaskService;
    private final TaskExecutionService taskExecutionService;
    private final DatasourceService datasourceService;
    private final TableSchemaService schemaService;
    private final IncrementalStrategyFactory strategyFactory;
    private final TypeMappingService typeMappingService;

    @Getter
    private volatile boolean running = false;
    private final AtomicBoolean stopped = new AtomicBoolean(false);
    private volatile int progress = 0;

    public IncrementalSyncEngine(SyncTaskService syncTaskService, TaskExecutionService taskExecutionService,
                                  DatasourceService datasourceService, IncrementalStrategyFactory strategyFactory,
                                  TypeMappingService typeMappingService) {
        this.syncTaskService = syncTaskService;
        this.taskExecutionService = taskExecutionService;
        this.datasourceService = datasourceService;
        this.typeMappingService = typeMappingService;
        this.schemaService = new TableSchemaService(datasourceService, typeMappingService);
        this.strategyFactory = strategyFactory;
    }

    @Override
    public void sync(SyncPipelineContext context) throws Exception {
        running = true;
        stopped.set(false);
        progress = 0;

        EtlSyncTask task = syncTaskService.getDetail(context.getTaskId());
        EtlTaskExecution execution = null;

        try {
            // 更新任务状态为运行中
            syncTaskService.updateStatus(task.getId(), TaskStatus.RUNNING.getCode());

            // 创建执行记录
            execution = taskExecutionService.createExecution(task.getId(), TriggerType.MANUAL.getCode());
            context.setExecutionId(execution.getId());
            context.setExecutionNo(execution.getExecutionNo());

            // 获取增量字段
            String incrementalField = task.getIncrementalField();
            if (StrUtil.isBlank(incrementalField)) {
                throw EtlException.configError("增量同步字段未配置");
            }

            // 获取增量策略类型，默认为TIMESTAMP
            String incrementalType = task.getIncrementalType();
            if (StrUtil.isBlank(incrementalType)) {
                incrementalType = "TIMESTAMP";
            }

            // 获取增量策略
            IncrementalStrategy strategy = strategyFactory.getStrategy(incrementalType);

            // 获取上次同步的断点值
            String lastValue = task.getIncrementalValue();
            if (StrUtil.isBlank(lastValue)) {
                lastValue = getInitialValue(task);
            }

            // 获取表配置
            JSONArray tableConfig = JsonUtil.parseArray(task.getTableConfig());
            if (tableConfig == null || tableConfig.isEmpty()) {
                throw EtlException.configError("表配置为空");
            }

            // 获取连接器
            DatabaseConnector sourceConnector = datasourceService.getConnector(task.getSourceDsId());
            DatabaseConnector targetConnector = datasourceService.getConnector(task.getTargetDsId());

            long totalRows = 0;
            long successRows = 0;
            long failedRows = 0;
            String maxIncrementalValue = lastValue;

            // 设置通用的同步上下文信息
            context.setIncrementalField(incrementalField);
            context.setDbType(sourceConnector.getDatabaseType());
            context.setBatchSize(task.getBatchSize() != null ? task.getBatchSize() : 1000);

            // 遍历每个表执行增量同步
            for (int i = 0; i < tableConfig.size(); i++) {
                if (stopped.get()) {
                    log.info("任务被停止: taskId={}", task.getId());
                    break;
                }

                JSONObject table = tableConfig.getJSONObject(i);
                String sourceTable = table.getString("sourceTable");
                String targetTable = table.getString("targetTable");

                context.setSourceTable(sourceTable);
                context.setTargetTable(targetTable);

                log.info("开始增量同步表: {} -> {}, 策略: {}, 增量字段: {}, 起始值: {}",
                    sourceTable, targetTable, incrementalType, incrementalField, lastValue);

                try {
                    // 自动创建目标表（默认启用）
                    boolean createTable = table.getBooleanValue("createTargetTable", true);
                    if (createTable) {
                        schemaService.createTargetTable(
                            task.getSourceDsId(), task.getTargetDsId(),
                            sourceTable, targetTable, task.getFieldMapping()
                        );
                    }

                    // 执行增量同步
                    IncrementalResult result = syncTableIncremental(
                        strategy,
                        sourceConnector, targetConnector,
                        context,
                        lastValue,
                        task.getBatchSize(), task.getFieldMapping()
                    );

                    totalRows += result.totalRows;
                    successRows += result.successRows;
                    failedRows += result.failedRows;

                    // 更新最大增量值
                    if (StrUtil.isNotBlank(result.maxValue) &&
                        strategy.comparePosition(result.maxValue, maxIncrementalValue) > 0) {
                        maxIncrementalValue = result.maxValue;
                    }

                    // 更新进度
                    int tableProgress = (int) ((i + 1.0) / tableConfig.size() * 100);
                    progress = tableProgress;

                    log.info("表增量同步完成: {} -> {}, 成功: {}, 失败: {}",
                        sourceTable, targetTable, result.successRows, result.failedRows);

                } catch (Exception e) {
                    log.error("表增量同步失败: {} -> {}", sourceTable, targetTable, e);
                    failedRows += 1;
                }
            }

            // 更新增量值
            if (!maxIncrementalValue.equals(lastValue)) {
                syncTaskService.updateIncrementalValue(task.getId(), maxIncrementalValue);
            }

            // 更新执行记录
            BigDecimal progressVal = totalRows > 0 ?
                new BigDecimal(successRows).divide(new BigDecimal(totalRows), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal(100)) : BigDecimal.ZERO;

            taskExecutionService.completeExecution(execution.getId(),
                stopped.get() ? ExecutionStatus.CANCELLED.getCode() : ExecutionStatus.SUCCESS.getCode(),
                null, maxIncrementalValue);
            taskExecutionService.updateProgress(execution.getId(), totalRows, successRows, failedRows, progressVal);

            // 更新任务状态
            syncTaskService.updateStatus(task.getId(), TaskStatus.STOPPED.getCode());

            log.info("增量同步完成: taskId={}, totalRows={}, successRows={}, failedRows={}, maxIncrementalValue={}",
                task.getId(), totalRows, successRows, failedRows, maxIncrementalValue);

        } catch (Exception e) {
            log.error("增量同步失败: taskId={}", task.getId(), e);
            if (execution != null) {
                taskExecutionService.completeExecution(execution.getId(),
                    ExecutionStatus.FAILED.getCode(), e.getMessage(), null);
            }
            syncTaskService.updateStatus(task.getId(), TaskStatus.STOPPED.getCode());
            throw e;
        } finally {
            running = false;
            progress = 100;
        }
    }

    /**
     * 执行单表增量同步
     */
    private IncrementalResult syncTableIncremental(IncrementalStrategy strategy,
                                                     DatabaseConnector sourceConnector,
                                                     DatabaseConnector targetConnector,
                                                     SyncPipelineContext context,
                                                     String lastValue,
                                                     Integer batchSize, String fieldMappingJson) throws Exception {
        IncrementalResult result = new IncrementalResult();

        Connection sourceConn = sourceConnector.getConnection();
        Connection targetConn = targetConnector.getConnection();

        try {
        // 使用策略构建查询SQL
        String selectSql = strategy.buildQuerySql(context, lastValue, sourceConnector.getDatabaseType());

        log.debug("增量查询SQL: {}", selectSql);

        // 构建插入SQL
        String insertSql = buildInsertSql(targetConnector, context.getTargetTable(), sourceConnector, context.getSourceTable());

        try (PreparedStatement selectStmt = sourceConn.prepareStatement(selectSql);
             PreparedStatement insertStmt = targetConn.prepareStatement(insertSql)) {

            ResultSet rs = selectStmt.executeQuery();
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            targetConn.setAutoCommit(false);
            int batchCount = 0;
            String maxValue = lastValue;

            while (rs.next() && !stopped.get()) {
                try {
                    for (int i = 1; i <= columnCount; i++) {
                        Object value = rs.getObject(i);
                        insertStmt.setObject(i, value);

                        // 记录增量字段的最大值
                        String columnName = metaData.getColumnName(i);
                        if (columnName.equalsIgnoreCase(context.getIncrementalField()) && value != null) {
                            String currentValue = strategy.extractPosition(rs, context.getIncrementalField());
                            if (strategy.comparePosition(currentValue, maxValue) > 0) {
                                maxValue = currentValue;
                            }
                        }
                    }
                    insertStmt.addBatch();
                    batchCount++;
                    result.totalRows++;
                    result.successRows++;

                    if (batchSize != null && batchCount >= batchSize) {
                        insertStmt.executeBatch();
                        targetConn.commit();
                        batchCount = 0;
                    }
                } catch (Exception e) {
                    log.error("写入数据失败", e);
                    result.failedRows++;
                }
            }

            // 执行剩余批次
            if (batchCount > 0) {
                insertStmt.executeBatch();
                targetConn.commit();
            }

            result.maxValue = maxValue;
            rs.close();
        } finally {
            try { targetConn.setAutoCommit(true); } catch (Exception e) { log.warn("恢复自动提交失败", e); }
        }
        } finally {
            try { sourceConn.close(); } catch (Exception e) { log.warn("关闭源连接失败", e); }
            try { targetConn.close(); } catch (Exception e) { log.warn("关闭目标连接失败", e); }
        }

        return result;
    }

    /**
     * 构建INSERT SQL，支持重复键处理
     */
    private String buildInsertSql(DatabaseConnector connector, String tableName,
                                   DatabaseConnector sourceConnector, String sourceTable) throws Exception {
        TableInfo tableInfo = sourceConnector.getTableInfo(sourceTable);
        if (tableInfo == null) {
            throw EtlException.metadataFailed(sourceTable, null);
        }

        StringBuilder sql = new StringBuilder();
        String dbType = connector.getDatabaseType();
        // 根据数据库类型确定标识符引用方式
        String leftQuote, rightQuote;
        if (dbType.equals("POSTGRESQL") || dbType.equals("ORACLE")) {
            leftQuote = rightQuote = "\"";
        } else if (dbType.equals("SQLSERVER")) {
            leftQuote = "[";
            rightQuote = "]";
        } else {
            leftQuote = rightQuote = "`";
        }

        List<String> columns = new ArrayList<>();
        List<String> placeholders = new ArrayList<>();

        for (ColumnInfo column : tableInfo.getColumns()) {
            columns.add(leftQuote + column.getColumnName() + rightQuote);
            placeholders.add("?");
       }

        sql.append("INSERT INTO ").append(leftQuote).append(tableName).append(rightQuote);
        sql.append(" (").append(String.join(", ", columns)).append(")");
        sql.append(" VALUES (").append(String.join(", ", placeholders)).append(")");

        // 处理重复键：MySQL使用ON DUPLICATE KEY UPDATE
        if ("MYSQL".equals(dbType)) {
            sql.append(" ON DUPLICATE KEY UPDATE ");
            List<String> updateClauses = new ArrayList<>();
            for (int i = 0; i < columns.size(); i++) {
                String col = columns.get(i);
                updateClauses.add(col + " = VALUES(" + col + ")");
            }
            sql.append(String.join(", ", updateClauses));
        } else if ("POSTGRESQL".equals(dbType)) {
            sql.append(" ON CONFLICT DO NOTHING");
        }

        return sql.toString();
    }

    /**
     * 获取初始增量值
     */
    private String getInitialValue(EtlSyncTask task) throws Exception {
        DatabaseConnector connector = datasourceService.getConnector(task.getSourceDsId());
        JSONArray tableConfig = JsonUtil.parseArray(task.getTableConfig());

        if (tableConfig != null && !tableConfig.isEmpty()) {
            String tableName = tableConfig.getJSONObject(0).getString("sourceTable");
            String quoteField = quoteIdentifier(connector, task.getIncrementalField());
            String quoteTable = quoteIdentifier(connector, tableName);

            String sql = String.format("SELECT MIN(%s) AS min_val FROM %s", quoteField, quoteTable);
            try (Connection conn = connector.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    Object minVal = rs.getObject("min_val");
                    return minVal != null ? minVal.toString() : "0";
                }
            }
        }
        return "0";
    }

    /**
     * 判断是否为数字类型
     * @deprecated 请使用IncrementalStrategy实现中的方法
     */
    @Deprecated
    private boolean isNumericValue(String value) {
        if (value == null) return false;
        try {
            Long.parseLong(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 比较增量值大小
     * @deprecated 请使用IncrementalStrategy实现中的方法
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    private int compareIncrementalValue(String v1, String v2) {
        if (v1 == null || v2 == null) return 0;

        try {
            // 尝试数字比较
            return Long.compare(Long.parseLong(v1), Long.parseLong(v2));
        } catch (NumberFormatException e) {
            // 字符串比较（如时间戳）
            return v1.compareTo(v2);
        }
    }

    /**
     * 引用标识符
     * @deprecated 请使用SqlBuilder中的方法
     */
    @Deprecated
    private String quoteIdentifier(DatabaseConnector connector, String identifier) {
        String dbType = connector.getDatabaseType();
        if (dbType.equals("POSTGRESQL") || dbType.equals("ORACLE")) {
            return "\"" + identifier + "\"";
        } else if (dbType.equals("SQLSERVER")) {
            return "[" + identifier + "]";
        }
        return "`" + identifier + "`";
    }

    @Override
    public void stop() {
        stopped.set(true);
        log.info("停止增量同步引擎");
    }

    @Override
    public int getProgress() {
        return progress;
    }

    /**
     * 增量同步结果
     */
    private static class IncrementalResult {
        long totalRows = 0;
        long successRows = 0;
        long failedRows = 0;
        String maxValue;
    }
}
