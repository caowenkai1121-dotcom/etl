package com.etl.engine.full;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.etl.common.callback.SyncLogCallback;
import com.etl.common.domain.ColumnInfo;
import com.etl.common.domain.SyncPipelineContext;
import com.etl.common.domain.TableInfo;
import com.etl.common.enums.ExecutionStatus;
import com.etl.common.enums.TaskStatus;
import com.etl.common.exception.EtlException;
import com.etl.common.utils.JsonUtil;
import com.etl.datasource.connector.DatabaseConnector;
import com.etl.datasource.service.DatasourceService;
import com.etl.common.enums.TriggerType;
import com.etl.engine.SyncEngine;
import com.etl.engine.entity.EtlSyncTask;
import com.etl.engine.entity.EtlTaskExecution;
import com.etl.engine.extract.DataHandler;
import com.etl.engine.extract.FullTableExtractor;
import com.etl.engine.load.BatchLoader;
import com.etl.engine.schema.TableSchemaService;
import com.etl.engine.schema.TypeMappingService;
import com.etl.engine.service.SyncTaskService;
import com.etl.engine.service.TaskExecutionService;
import com.etl.engine.transform.DataCleanser;
import com.etl.engine.transform.DataDesensitizer;
import com.etl.engine.transform.DataValidator;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 全量同步引擎（重构版）
 * 使用分层架构：extract -> transform -> load
 */
@Slf4j
@Component
public class FullSyncEngine implements SyncEngine {

    private final SyncTaskService syncTaskService;
    private final TaskExecutionService taskExecutionService;
    private final DatasourceService datasourceService;
    private final TableSchemaService schemaService;
    private final TypeMappingService typeMappingService;

    @Autowired(required = false)
    private DataCleanser dataCleanser;

    @Autowired(required = false)
    private DataDesensitizer dataDesensitizer;

    @Autowired(required = false)
    private DataValidator dataValidator;

    @Autowired(required = false)
    private BatchLoader batchLoader;

    @Value("${engine.feature.cleanser.enabled:false}")
    private boolean cleanserEnabled;

    @Value("${engine.feature.desensitize.enabled:false}")
    private boolean desensitizeEnabled;

    @Value("${engine.feature.validator.enabled:false}")
    private boolean validatorEnabled;

    @Getter
    private volatile boolean running = false;
    private final AtomicBoolean stopped = new AtomicBoolean(false);
    private volatile int progress = 0;

    public FullSyncEngine(SyncTaskService syncTaskService, TaskExecutionService taskExecutionService,
                         DatasourceService datasourceService, TypeMappingService typeMappingService) {
        this.syncTaskService = syncTaskService;
        this.taskExecutionService = taskExecutionService;
        this.datasourceService = datasourceService;
        this.typeMappingService = typeMappingService;
        this.schemaService = new TableSchemaService(datasourceService, typeMappingService);
    }

    /**
     * 推送日志
     */
    private void pushLog(SyncPipelineContext context, String level, String tableName, String message) {
        SyncLogCallback callback = context.getLogCallback();
        if (callback != null) {
            callback.log(context.getTaskId(), context.getExecutionId(), context.getTraceId(),
                level, "SYNC", tableName, message);
        }
    }

    /**
     * 推送进度
     */
    private void pushProgress(SyncPipelineContext context, int progressValue, String status) {
        SyncLogCallback callback = context.getLogCallback();
        if (callback != null) {
            callback.progress(context.getTaskId(), context.getExecutionId(), context.getTraceId(),
                progressValue, context.getTotalRows(), context.getSuccessRows(), context.getFailedRows(), status);
        }
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

            // 遍历每个表执行同步
            for (int i = 0; i < tableConfig.size(); i++) {
                if (stopped.get()) {
                    log.info("任务被停止: taskId={}", task.getId());
                    pushLog(context, "WARN", null, "任务被用户停止");
                    break;
                }

                JSONObject table = tableConfig.getJSONObject(i);
                String sourceTable = table.getString("sourceTable");
                String targetTable = table.getString("targetTable");

                context.setSourceTable(sourceTable);
                context.setTargetTable(targetTable);

                log.info("开始同步表: {} -> {}", sourceTable, targetTable);
                pushLog(context, "INFO", sourceTable, "开始同步表: " + sourceTable + " -> " + targetTable);

                try {
                    // 获取源表信息
                    TableInfo sourceTableInfo = sourceConnector.getTableInfo(sourceTable);
                    if (sourceTableInfo == null) {
                        log.warn("源表不存在: {}", sourceTable);
                        pushLog(context, "WARN", sourceTable, "源表不存在，跳过");
                        continue;
                    }

                    // 自动创建目标表
                    boolean createTable = table.getBooleanValue("createTargetTable", true);
                    if (createTable) {
                        schemaService.createTargetTable(
                            task.getSourceDsId(), task.getTargetDsId(),
                            sourceTable, targetTable, task.getFieldMapping()
                        );
                        pushLog(context, "INFO", targetTable, "自动创建目标表: " + targetTable);
                    }

                    // 执行数据同步（使用新的分层架构）
                    long[] result = syncTableWithLayers(sourceConnector, targetConnector, sourceTableInfo,
                        targetTable, task.getBatchSize(), task.getFieldMapping(), context);
                    successRows += result[0];
                    failedRows += result[1];

                    // 更新进度
                    int tableProgress = (int) ((i + 1.0) / tableConfig.size() * 100);
                    progress = tableProgress;

                    log.info("表同步完成: {} -> {}, 成功: {}, 失败: {}",
                        sourceTable, targetTable, result[0], result[1]);
                    pushLog(context, "INFO", sourceTable, "表同步完成，成功: " + result[0] + "，失败: " + result[1]);
                    pushProgress(context, tableProgress, "RUNNING");

                } catch (Exception e) {
                    log.error("表同步失败: {} -> {}", sourceTable, targetTable, e);
                    pushLog(context, "ERROR", sourceTable, "表同步失败: " + e.getMessage());
                    failedRows += 1;
                }
            }

            // 更新执行记录
            BigDecimal progressVal = totalRows > 0 ?
                new BigDecimal(successRows).divide(new BigDecimal(totalRows), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal(100)) : BigDecimal.ZERO;

            String finalStatus = stopped.get() ? ExecutionStatus.CANCELLED.getCode() : ExecutionStatus.SUCCESS.getCode();
            taskExecutionService.completeExecution(execution.getId(), finalStatus, null, null);
            taskExecutionService.updateProgress(execution.getId(), totalRows, successRows, failedRows, progressVal);

            // 推送最终进度
            pushProgress(context, 100, finalStatus);

            // 更新任务状态
            syncTaskService.updateStatus(task.getId(), TaskStatus.STOPPED.getCode());
            syncTaskService.updateSyncTime(task.getId(), null);

            log.info("全量同步完成: taskId={}, totalRows={}, successRows={}, failedRows={}",
                task.getId(), totalRows, successRows, failedRows);
            pushLog(context, "INFO", null, "全量同步完成，总行数: " + totalRows + "，成功: " + successRows + "，失败: " + failedRows);

        } catch (Exception e) {
            log.error("全量同步失败: taskId={}", task.getId(), e);
            pushLog(context, "ERROR", null, "全量同步失败: " + e.getMessage());
            pushProgress(context, progress, "FAILED");
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
     * 使用分层架构同步单表数据
     */
    private long[] syncTableWithLayers(DatabaseConnector sourceConnector, DatabaseConnector targetConnector,
                                     TableInfo sourceTableInfo, String targetTable, int batchSize,
                                     String fieldMappingJson, SyncPipelineContext context) throws Exception {
        AtomicLong successRows = new AtomicLong(0);
        AtomicLong failedRows = new AtomicLong(0);

        // 获取主键
        List<String> primaryKeys = sourceConnector.getPrimaryKeys(sourceTableInfo.getTableName());
        String primaryKey = !primaryKeys.isEmpty() ? primaryKeys.get(0) : "id";

        // 创建抽取器
        FullTableExtractor extractor = new FullTableExtractor(sourceConnector,
            sourceTableInfo.getTableName(), batchSize, primaryKey, stopped);

        // 创建数据处理器
        DataHandler handler = batch -> {
            try {
                List<Map<String, Object>> processedData = processBatch(batch, fieldMappingJson);

                // 加载数据
                long loadedRows = 0;
                if (batchLoader != null) {
                    BatchLoader.LoadConfig loadConfig = BatchLoader.LoadConfig.defaultConfig();
                    if (!primaryKeys.isEmpty()) {
                        loadConfig = BatchLoader.LoadConfig.upsertConfig(primaryKeys.toArray(new String[0]));
                    }
                    loadedRows = batchLoader.load(targetConnector, targetTable, processedData, loadConfig);
                } else {
                    // 回退到旧的加载方式
                    loadedRows = loadFallback(targetConnector, targetTable, processedData);
                }

                successRows.addAndGet(loadedRows);

                // 更新进度
                context.setProcessedRows(successRows.get());
                context.setSuccessRows(successRows.get());

            } catch (Exception e) {
                log.error("处理批次数据失败", e);
                failedRows.addAndGet(batch.size());
            }
        };

        // 执行抽取
        extractor.extract(context, handler);

        return new long[]{successRows.get(), failedRows.get()};
    }

    /**
     * 处理批次数据（清洗、脱敏、校验）
     */
    private List<Map<String, Object>> processBatch(List<Map<String, Object>> batch, String fieldMappingJson) {
        List<Map<String, Object>> result = batch;

        // 1. 数据清洗
        if (cleanserEnabled && dataCleanser != null) {
            result = dataCleanser.cleanse(result, DataCleanser.CleanseConfig.defaultConfig());
        }

        // 2. 数据脱敏
        if (desensitizeEnabled && dataDesensitizer != null) {
            // TODO: 从任务配置中获取脱敏规则
            result = dataDesensitizer.desensitize(result, Collections.emptyList());
        }

        // 3. 数据校验
        if (validatorEnabled && dataValidator != null) {
            // TODO: 从任务配置中获取校验规则
            com.etl.engine.transform.DataValidator.ValidationResult validationResult =
                dataValidator.validate(result, Collections.emptyList());
            result = validationResult.getValidData();
        }

        // 4. 字段映射
        if (StrUtil.isNotBlank(fieldMappingJson)) {
            result = applyFieldMapping(result, fieldMappingJson);
        }

        return result;
    }

    /**
     * 应用字段映射
     */
    private List<Map<String, Object>> applyFieldMapping(List<Map<String, Object>> data, String fieldMappingJson) {
        JSONArray mappingArray = JsonUtil.parseArray(fieldMappingJson);
        if (mappingArray == null || mappingArray.isEmpty()) {
            return data;
        }

        Map<String, String> fieldMapping = new HashMap<>();
        for (int i = 0; i < mappingArray.size(); i++) {
            JSONObject mapping = mappingArray.getJSONObject(i);
            String sourceField = mapping.getString("sourceField");
            String targetField = mapping.getString("targetField");
            fieldMapping.put(sourceField, targetField);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : data) {
            Map<String, Object> newRow = new HashMap<>();
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                String targetField = fieldMapping.getOrDefault(entry.getKey(), entry.getKey());
                newRow.put(targetField, entry.getValue());
            }
            result.add(newRow);
        }

        return result;
    }

    /**
     * 回退加载方式（当 BatchLoader 不可用时）
     */
    private long loadFallback(DatabaseConnector targetConnector, String targetTable,
                             List<Map<String, Object>> data) throws Exception {
        if (data == null || data.isEmpty()) {
            return 0;
        }

        List<String> columns = new ArrayList<>(data.get(0).keySet());
        String placeholders = String.join(", ", Collections.nCopies(columns.size(), "?"));
        String columnsStr = String.join(", ", columns);
        String sql = buildInsertSql(targetConnector, targetTable, columnsStr, placeholders);

        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = targetConnector.getConnection();
            conn.setAutoCommit(false);
            stmt = conn.prepareStatement(sql);

            for (Map<String, Object> row : data) {
                int index = 1;
                for (String column : columns) {
                    stmt.setObject(index++, row.get(column));
                }
                stmt.addBatch();
            }

            int[] results = stmt.executeBatch();
            conn.commit();

            long count = 0;
            for (int result : results) {
                count += Math.max(1, result);
            }
            return count;
        } finally {
            if (stmt != null) try { stmt.close(); } catch (Exception ignored) {}
            if (conn != null) try { conn.setAutoCommit(true); conn.close(); } catch (Exception ignored) {}
        }
    }

    /**
     * 同步单表数据（旧方法，保留用于兼容性）
     */
    @Deprecated
    private long[] syncTable(DatabaseConnector sourceConnector, DatabaseConnector targetConnector,
                             TableInfo sourceTableInfo, String targetTable, Integer batchSize,
                             String fieldMappingJson, SyncPipelineContext context) throws Exception {
        long successRows = 0;
        long failedRows = 0;

        Connection sourceConn = sourceConnector.getConnection();
        Connection targetConn = targetConnector.getConnection();

        try {
        // 获取字段映射
        List<String> sourceColumns = new ArrayList<>();
        List<String> targetColumns = new ArrayList<>();

        if (StrUtil.isNotBlank(fieldMappingJson)) {
            JSONArray mappingArray = JsonUtil.parseArray(fieldMappingJson);
            if (mappingArray != null) {
                for (int i = 0; i < mappingArray.size(); i++) {
                    JSONObject mapping = mappingArray.getJSONObject(i);
                    sourceColumns.add(mapping.getString("sourceField"));
                    targetColumns.add(mapping.getString("targetField"));
                }
            }
        }

        // 如果没有配置映射，使用所有字段
        if (sourceColumns.isEmpty()) {
            for (ColumnInfo column : sourceTableInfo.getColumns()) {
                sourceColumns.add(column.getColumnName());
                targetColumns.add(column.getColumnName());
            }
        }

        // 构建查询SQL - 使用源数据库的引号风格
        String sourceColumnsStr = String.join(", ", sourceColumns.stream()
            .map(c -> quoteColumn(sourceConnector, c))
            .toList());

        String selectSql = String.format("SELECT %s FROM %s",
            sourceColumnsStr, quoteIdentifier(sourceConnector, sourceTableInfo.getTableName()));

        // 构建插入SQL - 使用目标数据库的引号风格
        String targetColumnsStr = String.join(", ", targetColumns.stream()
            .map(c -> quoteColumn(targetConnector, c))
            .toList());

        String placeholders = String.join(", ", Collections.nCopies(targetColumns.size(), "?"));

        // 使用 INSERT IGNORE 或 ON DUPLICATE KEY UPDATE 处理重复数据
        String insertSql = buildInsertSql(targetConnector, targetTable, targetColumnsStr, placeholders);

        // 分批读取和写入
        try (PreparedStatement selectStmt = sourceConn.prepareStatement(selectSql,
             ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
             PreparedStatement insertStmt = targetConn.prepareStatement(insertSql)) {

            selectStmt.setFetchSize(batchSize);
            ResultSet rs = selectStmt.executeQuery();

            targetConn.setAutoCommit(false);
            int batchCount = 0;

            while (rs.next() && !stopped.get()) {
                try {
                    for (int i = 0; i < targetColumns.size(); i++) {
                        Object value = rs.getObject(i + 1);
                        insertStmt.setObject(i + 1, value);
                    }
                    insertStmt.addBatch();
                    batchCount++;
                    successRows++;

                    if (batchCount >= batchSize) {
                        insertStmt.executeBatch();
                        targetConn.commit();
                        batchCount = 0;

                        // 更新进度
                        context.setProcessedRows(successRows);
                        context.setSuccessRows(successRows);
                    }
                } catch (Exception e) {
                    log.error("写入数据失败", e);
                    failedRows++;
                }
            }

            // 执行剩余批次
            if (batchCount > 0) {
                insertStmt.executeBatch();
                targetConn.commit();
            }

            rs.close();
        } finally {
            try { targetConn.setAutoCommit(true); } catch (Exception e) { log.warn("恢复自动提交失败", e); }
        }
        } finally {
            try { sourceConn.close(); } catch (Exception e) { log.warn("关闭源连接失败", e); }
            try { targetConn.close(); } catch (Exception e) { log.warn("关闭目标连接失败", e); }
        }

        return new long[]{successRows, failedRows};
    }

    private String quoteIdentifier(DatabaseConnector connector, String identifier) {
        String dbType = connector.getDatabaseType();
        if (dbType.equals("POSTGRESQL") || dbType.equals("ORACLE")) {
            return "\"" + identifier + "\"";
        } else if (dbType.equals("SQLSERVER")) {
            return "[" + identifier + "]";
        }
        return "`" + identifier + "`";
    }

    /**
     * 引用列名
     */
    private String quoteColumn(DatabaseConnector connector, String columnName) {
        String dbType = connector.getDatabaseType();
        if (dbType.equals("POSTGRESQL") || dbType.equals("ORACLE")) {
            return "\"" + columnName + "\"";
        } else if (dbType.equals("SQLSERVER")) {
            return "[" + columnName + "]";
        } else if (dbType.equals("DORIS")) {
            return "`" + columnName + "`";
        }
        return "`" + columnName + "`";
    }

    /**
     * 构建INSERT SQL，支持重复键处理
     */
    @Deprecated
    private String buildInsertSql(DatabaseConnector connector, String tableName,
                                   String columnsStr, String placeholders) {
        String quotedTable = quoteIdentifier(connector, tableName);
        String dbType = connector.getDatabaseType();

        if ("MYSQL".equals(dbType)) {
            // MySQL使用 INSERT IGNORE
            return String.format("INSERT IGNORE INTO %s (%s) VALUES (%s)",
                quotedTable, columnsStr, placeholders);
        } else if ("DORIS".equals(dbType) || "CLICKHOUSE".equals(dbType)) {
            // Doris 和 ClickHouse 支持 INSERT
            return String.format("INSERT INTO %s (%s) VALUES (%s)",
                quotedTable, columnsStr, placeholders);
        } else if ("POSTGRESQL".equals(dbType)) {
            // PostgreSQL使用 ON CONFLICT DO NOTHING
            return String.format("INSERT INTO %s (%s) VALUES (%s) ON CONFLICT DO NOTHING",
                quotedTable, columnsStr, placeholders);
        } else if ("ORACLE".equals(dbType)) {
            // Oracle 使用 MERGE INTO 处理重复数据
            return String.format("INSERT INTO %s (%s) VALUES (%s)",
                quotedTable, columnsStr, placeholders);
        } else if ("SQLSERVER".equals(dbType)) {
            // SQL Server 使用 INSERT + IGNORE_DUP_KEY
            return String.format("INSERT INTO %s (%s) VALUES (%s)",
                quotedTable, columnsStr, placeholders);
        }

        // 默认普通INSERT
        return String.format("INSERT INTO %s (%s) VALUES (%s)",
            quotedTable, columnsStr, placeholders);
    }

    @Override
    public void stop() {
        stopped.set(true);
        log.info("停止全量同步引擎");
    }

    @Override
    public int getProgress() {
        return progress;
    }
}
