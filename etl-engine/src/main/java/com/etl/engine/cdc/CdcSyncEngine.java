package com.etl.engine.cdc;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.etl.common.domain.CdcEvent;
import com.etl.common.domain.SyncPipelineContext;
import com.etl.common.enums.*;
import com.etl.common.utils.JsonUtil;
import com.etl.datasource.connector.DatabaseConnector;
import com.etl.datasource.service.DatasourceService;
import com.etl.engine.SyncEngine;
import com.etl.engine.entity.EtlCdcConfig;
import com.etl.engine.entity.EtlCdcPosition;
import com.etl.engine.entity.EtlSyncTask;
import com.etl.engine.entity.EtlTaskExecution;
import com.etl.engine.mapper.CdcPositionMapper;
import com.etl.engine.service.CdcConfigService;
import com.etl.engine.service.SyncTaskService;
import com.etl.engine.service.TaskExecutionService;
import com.etl.engine.schema.TableSchemaService;
import com.etl.engine.schema.TypeMappingService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * CDC实时同步引擎
 * 通过Kafka消费Debezium发送的变更事件
 * 支持位点管理和断点续传
 *
 * 支持两种消息格式：
 * 1. Debezium消息格式（推荐）：
 * {
 *   "payload": {
 *     "before": null,              // 变更前数据
 *     "after": {"id": 1, ...},     // 变更后数据
 *     "source": {
 *       "db": "source_db",         // 数据库名
 *       "table": "users",          // 表名
 *       "ts_ms": 1234567890        // 时间戳
 *     },
 *     "op": "c",                   // c=insert, u=update, d=delete
 *     "ts_ms": 1234567890
 *   }
 * }
 *
 * 2. Canal消息格式（兼容旧版）：
 * {
 *   "data": [{...}],           // 变更数据数组
 *   "database": "source_db",   // 数据库名
 *   "es": 1234567890,          // 事件时间戳
 *   "id": 1,                   // 事件ID
 *   "isDdl": false,            // 是否DDL语句
 *   "old": [{...}],            // 旧数据(UPDATE时)
 *   "pkNames": ["id"],         // 主键列名
 *   "sql": "",                 // SQL语句
 *   "table": "users",          // 表名
 *   "ts": 1234567890,          // 处理时间戳
 *   "type": "INSERT"           // 操作类型: INSERT/UPDATE/DELETE
 * }
 */
@Slf4j
public class CdcSyncEngine implements SyncEngine {

    private final SyncTaskService syncTaskService;
    private final TaskExecutionService taskExecutionService;
    private final DatasourceService datasourceService;
    private final CdcPositionMapper cdcPositionMapper;
    private final TableSchemaService schemaService;
    private final TypeMappingService typeMappingService;

    @Getter
    private volatile boolean running = false;
    private final AtomicBoolean stopped = new AtomicBoolean(false);
    private final AtomicBoolean shutdownCalled = new AtomicBoolean(false);
    private volatile int progress = 0;

    private KafkaConsumer<String, String> kafkaConsumer;
    private ExecutorService executorService;

    // 统计计数器
    private final AtomicLong processedCount = new AtomicLong(0);
    private final AtomicLong successCount = new AtomicLong(0);
    private final AtomicLong failCount = new AtomicLong(0);

    // 位点管理 - 每个分区的最新offset
    private final Map<TopicPartition, Long> committedOffsets = new ConcurrentHashMap<>();
    private final Map<TopicPartition, Long> pendingOffsets = new ConcurrentHashMap<>();

    // 表配置缓存 - 存储每个表的完整配置（源表名 -> 配置信息）
    private final Map<String, TableConfigInfo> tableConfigMap = new ConcurrentHashMap<>();

    // 已创建的表集合，避免重复创建
    private final Set<String> createdTables = ConcurrentHashMap.newKeySet();

    // 位点提交间隔
    private static final int COMMIT_INTERVAL = 100;
    private static final long COMMIT_TIMEOUT_MS = 5000;
    private long lastCommitTime = System.currentTimeMillis();

    // Kafka配置
    private final String kafkaBootstrapServers;
    private final String canalTopic;
    private final CdcConfigService cdcConfigService;

    // 任务信息缓存
    private Long taskId;
    private Long sourceDsId;
    private Long targetDsId;

    // Kafka消费者锁 - 确保线程安全
    private final Object consumerLock = new Object();

    public CdcSyncEngine(SyncTaskService syncTaskService, TaskExecutionService taskExecutionService,
                          DatasourceService datasourceService, CdcPositionMapper cdcPositionMapper,
                          String kafkaBootstrapServers, String canalTopic, TypeMappingService typeMappingService,
                          CdcConfigService cdcConfigService) {
        this.syncTaskService = syncTaskService;
        this.taskExecutionService = taskExecutionService;
        this.datasourceService = datasourceService;
        this.cdcPositionMapper = cdcPositionMapper;
        this.kafkaBootstrapServers = kafkaBootstrapServers;
        this.canalTopic = canalTopic;
        this.typeMappingService = typeMappingService;
        this.cdcConfigService = cdcConfigService;
        this.schemaService = new TableSchemaService(datasourceService, typeMappingService);
    }

    /**
     * 从CDC配置中获取实际的Kafka topic前缀
     */
    private String resolveKafkaTopicPrefix(Long srcDsId) {
        if (cdcConfigService != null) {
            try {
                EtlCdcConfig cdcConfig = cdcConfigService.getByDatasourceId(srcDsId);
                if (cdcConfig != null && StrUtil.isNotBlank(cdcConfig.getKafkaTopicPrefix())) {
                    log.info("使用CDC配置的topic前缀: {}", cdcConfig.getKafkaTopicPrefix());
                    return cdcConfig.getKafkaTopicPrefix();
                }
            } catch (Exception e) {
                log.warn("获取CDC配置失败，使用默认topic前缀: {}", canalTopic);
            }
        }
        return canalTopic;
    }

    /**
     * 表配置信息
     */
    private static class TableConfigInfo {
        String sourceTable;
        String targetTable;
        boolean createTargetTable;

        TableConfigInfo(String sourceTable, String targetTable, boolean createTargetTable) {
            this.sourceTable = sourceTable;
            this.targetTable = targetTable;
            this.createTargetTable = createTargetTable;
        }
    }

    @Override
    public void sync(SyncPipelineContext context) throws Exception {
        running = true;
        stopped.set(false);
        progress = 0;
        processedCount.set(0);
        successCount.set(0);
        failCount.set(0);

        EtlSyncTask task = syncTaskService.getDetail(context.getTaskId());
        EtlTaskExecution execution = null;

        try {
            // 缓存任务信息
            this.taskId = task.getId();
            this.sourceDsId = task.getSourceDsId();
            this.targetDsId = task.getTargetDsId();

            // 更新任务状态为运行中
            syncTaskService.updateStatus(task.getId(), TaskStatus.RUNNING.getCode());

            // 创建执行记录
            execution = taskExecutionService.createExecution(
                task.getId(), TriggerType.CDC.getCode());
            context.setExecutionId(execution.getId());

            // 获取表配置并解析为映射
            JSONArray tableConfig = JsonUtil.parseArray(task.getTableConfig());
            parseAndCacheTableConfig(tableConfig);

            // 初始化Kafka消费者
            initKafkaConsumer(task.getId());

            // 检查是否有历史位点，如果没有则执行全量初始化
            boolean hasPosition = checkHasPosition(task.getId());
            if (!hasPosition) {
                log.info("CDC首次启动，执行全量数据初始化: taskId={}", task.getId());
                fullSyncInitialization(task.getBatchSize());
            }

            // 尝试从数据库恢复位点
            restorePosition(task.getId());

            // 启动事件处理线程
            executorService = Executors.newFixedThreadPool(4);
            startEventProcessing();

            // 启动位点保存线程
            startPositionPersistThread(task.getId(), task.getSourceDsId());

            log.info("CDC同步引擎启动成功: taskId={}, topic={}, tables={}",
                task.getId(), canalTopic, tableConfigMap.keySet());

            // 持续运行直到停止
            while (!stopped.get()) {
                Thread.sleep(1000);
                // 更新进度
                progress = running ? 50 : 100;
            }

        } catch (Exception e) {
            log.error("CDC同步失败: taskId={}", task.getId(), e);
            if (execution != null) {
                taskExecutionService.completeExecution(execution.getId(),
                    ExecutionStatus.FAILED.getCode(), e.getMessage(), null);
            }
            throw e;
        } finally {
            shutdown();
            syncTaskService.updateStatus(task.getId(), TaskStatus.STOPPED.getCode());
            running = false;
        }
    }

    /**
     * 检查是否有历史位点
     */
    private boolean checkHasPosition(Long taskId) {
        try {
            Long count = cdcPositionMapper.selectCount(
                new LambdaQueryWrapper<EtlCdcPosition>()
                    .eq(EtlCdcPosition::getTaskId, taskId));
            return count != null && count > 0;
        } catch (Exception e) {
            log.warn("检查位点失败: taskId={}", taskId, e);
            return false;
        }
    }

    /**
     * 全量数据初始化 - CDC首次启动时同步历史数据
     */
    private void fullSyncInitialization(Integer batchSize) throws Exception {
        log.info("开始全量数据初始化: tables={}", tableConfigMap.keySet());
        long totalRows = 0;
        long successRows = 0;
        long startTime = System.currentTimeMillis();

        DatabaseConnector sourceConnector = datasourceService.getConnector(sourceDsId);
        DatabaseConnector targetConnector = datasourceService.getConnector(targetDsId);

        for (TableConfigInfo config : tableConfigMap.values()) {
            if (stopped.get()) break;

            String sourceTable = config.sourceTable;
            String targetTable = config.targetTable;

            try {
                log.info("全量初始化表: {} -> {}", sourceTable, targetTable);

                // 检查并创建目标表（强制重新验证，不使用createdTables缓存）
                if (config.createTargetTable) {
                    createdTables.remove(targetTable);
                    ensureTargetTableExists(sourceTable, targetTable);
                }

                // 检查目标表是否已有数据
                long targetCount = getTargetRowCount(targetConnector, targetTable);
                if (targetCount > 0) {
                    log.info("目标表已有{}条数据，跳过全量初始化: {}", targetCount, targetTable);
                    continue;
                }

                // 执行全量同步
                long rows = fullSyncTable(sourceConnector, targetConnector, sourceTable, targetTable, batchSize);
                totalRows += rows;
                successRows += rows;

                log.info("表全量初始化完成: {} -> {}, rows={}", sourceTable, targetTable, rows);

            } catch (Exception e) {
                log.error("表全量初始化失败: {} -> {}", sourceTable, targetTable, e);
                failCount.incrementAndGet();
            }
        }

        processedCount.addAndGet(successRows);
        successCount.addAndGet(successRows);

        long duration = System.currentTimeMillis() - startTime;
        log.info("全量数据初始化完成: totalRows={}, successRows={}, duration={}ms",
            totalRows, successRows, duration);
    }

    /**
     * 获取目标表行数
     */
    private long getTargetRowCount(DatabaseConnector connector, String tableName) {
        try {
            return connector.getRowCount(tableName);
        } catch (Exception e) {
            log.warn("获取目标表行数失败: {}", tableName, e);
            return 0;
        }
    }

    /**
     * 全量同步单表数据
     */
    private long fullSyncTable(DatabaseConnector sourceConnector, DatabaseConnector targetConnector,
                               String sourceTable, String targetTable, Integer batchSize) throws Exception {
        long totalRows = 0;
        String quote = targetConnector.getDatabaseType().equals("POSTGRESQL") ? "\"" : "`";

        // 获取源表列信息
        var tableInfo = sourceConnector.getTableInfo(sourceTable);
        if (tableInfo == null) {
            throw new RuntimeException("源表不存在: " + sourceTable);
        }

        List<String> columns = new ArrayList<>();
        for (var col : tableInfo.getColumns()) {
            columns.add(col.getColumnName());
        }

        String sourceColumnsStr = String.join(", ", columns);
        String targetColumnsStr = String.join(", ", columns.stream().map(c -> quote + c + quote).toList());
        String placeholders = String.join(", ", Collections.nCopies(columns.size(), "?"));

        // 构建查询SQL
        String selectSql = String.format("SELECT %s FROM %s", sourceColumnsStr,
            sourceConnector.getDatabaseType().equals("POSTGRESQL") ? "\"" + sourceTable + "\"" : "`" + sourceTable + "`");

        // 构建插入SQL
        String insertSql = buildFullSyncInsertSql(targetConnector, targetTable, targetColumnsStr, placeholders, columns, tableInfo);

        Connection sourceConn = null;
        Connection targetConn = null;

        try {
            sourceConn = sourceConnector.getConnection();
            targetConn = targetConnector.getConnection();

            try (PreparedStatement selectStmt = sourceConn.prepareStatement(selectSql,
                    ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
                 PreparedStatement insertStmt = targetConn.prepareStatement(insertSql)) {

                selectStmt.setFetchSize(batchSize != null ? batchSize : 1000);
                ResultSet rs = selectStmt.executeQuery();

                targetConn.setAutoCommit(false);
                int batchCount = 0;

                while (rs.next() && !stopped.get()) {
                    for (int i = 0; i < columns.size(); i++) {
                        Object value = rs.getObject(i + 1);
                        insertStmt.setObject(i + 1, value);
                    }
                    insertStmt.addBatch();
                    batchCount++;
                    totalRows++;

                    if (batchCount >= (batchSize != null ? batchSize : 1000)) {
                        insertStmt.executeBatch();
                        targetConn.commit();
                        batchCount = 0;
                        log.debug("全量同步批次提交: table={}, rows={}", targetTable, totalRows);
                    }
                }

                // 执行剩余批次
                if (batchCount > 0) {
                    insertStmt.executeBatch();
                    targetConn.commit();
                }

                rs.close();
            }
        } finally {
            if (sourceConn != null) {
                try { sourceConn.close(); } catch (Exception ignored) {}
            }
            if (targetConn != null) {
                try { targetConn.setAutoCommit(true); targetConn.close(); } catch (Exception ignored) {}
            }
        }

        return totalRows;
    }

    /**
     * 构建全量同步INSERT SQL
     */
    private String buildFullSyncInsertSql(DatabaseConnector connector, String tableName,
                                          String columnsStr, String placeholders,
                                          List<String> columns, com.etl.common.domain.TableInfo tableInfo) {
        String quote = connector.getDatabaseType().equals("POSTGRESQL") ? "\"" : "`";
        String dbType = connector.getDatabaseType();

        // 获取主键列
        List<String> pkColumns = tableInfo.getPrimaryKeys();

        if ("MYSQL".equals(dbType)) {
            // MySQL使用 INSERT ... ON DUPLICATE KEY UPDATE
            StringBuilder sql = new StringBuilder();
            sql.append("INSERT INTO ").append(quote).append(tableName).append(quote).append(" (");
            sql.append(columnsStr).append(") VALUES (").append(placeholders).append(")");
            if (!pkColumns.isEmpty()) {
                sql.append(" ON DUPLICATE KEY UPDATE ");
                List<String> updateClauses = new ArrayList<>();
                for (String col : columns) {
                    if (!pkColumns.contains(col)) {
                        updateClauses.add(quote + col + quote + " = VALUES(" + quote + col + quote + ")");
                    }
                }
                if (!updateClauses.isEmpty()) {
                    sql.append(String.join(", ", updateClauses));
                } else {
                    // 如果没有非主键列，使用简单形式
                    return String.format("INSERT IGNORE INTO %s%s%s (%s) VALUES (%s)",
                        quote, tableName, quote, columnsStr, placeholders);
                }
            }
            return sql.toString();
        } else if ("POSTGRESQL".equals(dbType)) {
            // PostgreSQL使用 ON CONFLICT DO UPDATE
            StringBuilder sql = new StringBuilder();
            sql.append("INSERT INTO ").append(quote).append(tableName).append(quote).append(" (");
            sql.append(columnsStr).append(") VALUES (").append(placeholders).append(")");
            if (!pkColumns.isEmpty()) {
                sql.append(" ON CONFLICT (");
                sql.append(String.join(", ", pkColumns.stream().map(c -> quote + c + quote).toList()));
                sql.append(") DO UPDATE SET ");
                List<String> updateClauses = new ArrayList<>();
                for (String col : columns) {
                    if (!pkColumns.contains(col)) {
                        updateClauses.add(quote + col + quote + " = EXCLUDED." + quote + col + quote);
                    }
                }
                if (!updateClauses.isEmpty()) {
                    sql.append(String.join(", ", updateClauses));
                } else {
                    sql.append(" ON CONFLICT DO NOTHING");
                }
            } else {
                sql.append(" ON CONFLICT DO NOTHING");
            }
            return sql.toString();
        } else {
            // Doris和其他数据库
            return String.format("INSERT INTO %s%s%s (%s) VALUES (%s)",
                quote, tableName, quote, columnsStr, placeholders);
        }
    }

    /**
     * 解析并缓存表配置
     */
    private void parseAndCacheTableConfig(JSONArray tableConfig) {
        if (tableConfig == null) return;

        for (int i = 0; i < tableConfig.size(); i++) {
            JSONObject table = tableConfig.getJSONObject(i);
            String sourceTable = table.getString("sourceTable");
            String targetTable = table.getString("targetTable");
            boolean createTargetTable = table.getBooleanValue("createTargetTable", true);

            if (StrUtil.isNotBlank(sourceTable)) {
                String target = StrUtil.isNotBlank(targetTable) ? targetTable : sourceTable;
                tableConfigMap.put(sourceTable, new TableConfigInfo(sourceTable, target, createTargetTable));
                log.info("CDC表配置: {} -> {}, autoCreate={}", sourceTable, target, createTargetTable);
            }
        }
    }

    /**
     * 初始化Kafka消费者
     */
    private void initKafkaConsumer(Long taskId) {
        // Spring Boot fat jar嵌套类加载问题：KafkaConsumer通过Class.forName加载deserializer，
        // 需要确保线程上下文ClassLoader能找到嵌套JAR中的类
        ClassLoader originalCl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        try {
        Properties props = new Properties();
        props.put("bootstrap.servers", kafkaBootstrapServers);
        props.put("group.id", "etl-cdc-group-" + taskId);
        props.put("enable.auto.commit", "false");
        props.put("auto.offset.reset", "earliest");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        // 消费配置
        props.put("max.poll.records", "500");
        props.put("max.poll.interval.ms", "300000");
        props.put("session.timeout.ms", "30000");
        props.put("heartbeat.interval.ms", "10000");
        // 隔离级别 - 读取已提交的消息
        props.put("isolation.level", "read_committed");

        kafkaConsumer = new KafkaConsumer<>(props);

        // 只订阅配置的表对应的Topic
        // Debezium的Topic格式: {prefix}.{database}.{table}
        // 例如: etl-mysql-21.xh_dms.t_after_sales_order_header
        List<String> topicsToSubscribe = new ArrayList<>();

        // 获取源数据库连接器以获取数据库名称
        DatabaseConnector sourceConnector = datasourceService.getConnector(sourceDsId);
        String databaseName = sourceConnector.getDatabaseName();

        // 从CDC配置获取实际的topic前缀（匹配Debezium connector的serverName）
        String actualTopicPrefix = resolveKafkaTopicPrefix(sourceDsId);

        // 构建订阅的 topic 列表
        for (String sourceTable : tableConfigMap.keySet()) {
            String topic = actualTopicPrefix + "." + databaseName + "." + sourceTable;
            topicsToSubscribe.add(topic);
        }

        if (topicsToSubscribe.isEmpty()) {
            // 如果没有配置表，使用正则表达式订阅（向后兼容）
            Pattern topicPattern = Pattern.compile("^" + Pattern.quote(actualTopicPrefix) + "\\..+");
            kafkaConsumer.subscribe(topicPattern);
            log.info("Kafka消费者已初始化: topicPattern={}.*, groupId={}", actualTopicPrefix, props.get("group.id"));
        } else {
            // 只订阅配置的表的 topic
            kafkaConsumer.subscribe(topicsToSubscribe);
            log.info("Kafka消费者已初始化: topics={}, groupId={}", topicsToSubscribe, props.get("group.id"));
        }
        } finally {
            Thread.currentThread().setContextClassLoader(originalCl);
        }
    }

    /**
     * 从数据库恢复位点
     * 支持多主题位点恢复（Debezium格式: {prefix}.{database}.{table}）
     */
    private void restorePosition(Long taskId) {
        try {
            List<EtlCdcPosition> positions = cdcPositionMapper.selectList(
                new LambdaQueryWrapper<EtlCdcPosition>()
                    .eq(EtlCdcPosition::getTaskId, taskId)
                    .eq(EtlCdcPosition::getPositionType, "KAFKA_OFFSET"));

            if (positions.isEmpty()) {
                log.info("没有找到历史位点，从头开始消费");
                return;
            }

            // 等待分区分配完成 - 调用poll()触发分区分配
            synchronized (consumerLock) {
                log.info("等待Kafka分区分配完成...");
                kafkaConsumer.poll(Duration.ofMillis(100));
                // 确保分区分配完成，最多等待10秒
                int maxRetries = 100;
                int retry = 0;
                while (kafkaConsumer.assignment().isEmpty() && retry < maxRetries) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.warn("等待分区分配被中断");
                        return;
                    }
                    retry++;
                }
                if (kafkaConsumer.assignment().isEmpty()) {
                    log.warn("等待分区分配超时，无法恢复位点");
                    return;
                }
                log.info("分区分配完成，已分配{}个分区", kafkaConsumer.assignment().size());
            }

            for (EtlCdcPosition pos : positions) {
                if (StrUtil.isNotBlank(pos.getPositionValue()) && StrUtil.isNotBlank(pos.getTableName())) {
                    // 解析位点: partition:offset格式
                    String[] parts = pos.getPositionValue().split(":");
                    if (parts.length == 2) {
                        int partition = Integer.parseInt(parts[0]);
                        long offset = Long.parseLong(parts[1]);

                        // tableName字段存储了完整的Kafka主题名称
                        TopicPartition tp = new TopicPartition(pos.getTableName(), partition);

                        // 检查分区是否已分配
                        if (kafkaConsumer.assignment().contains(tp)) {
                            kafkaConsumer.seek(tp, offset);
                            committedOffsets.put(tp, offset);
                            log.info("恢复位点: topic={}, partition={}, offset={}", pos.getTableName(), partition, offset);
                        } else {
                            log.warn("分区未分配，跳过位点恢复: topic={}, partition={}", pos.getTableName(), partition);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("恢复位点失败，从头开始消费", e);
        }
    }

    /**
     * 启动事件处理
     */
    private void startEventProcessing() {
        executorService.submit(() -> {
            log.info("CDC事件处理线程启动: taskId={}", taskId);
            while (!stopped.get() && !Thread.currentThread().isInterrupted()) {
                try {
                    ConsumerRecords<String, String> records;
                    synchronized (consumerLock) {
                        if (kafkaConsumer == null || stopped.get()) {
                            break;
                        }
                        records = kafkaConsumer.poll(Duration.ofSeconds(1));
                    }

                    if (records.isEmpty()) {
                        continue;
                    }

                    log.debug("Kafka轮询收到{}条消息: taskId={}", records.count(), taskId);

                    for (ConsumerRecord<String, String> record : records) {
                        if (stopped.get()) break;

                        try {
                            // 从主题名称提取表名（格式: prefix.database.table）
                            String topic = record.topic();
                            String[] topicParts = topic.split("\\.");
                            if (topicParts.length >= 3) {
                                currentTableFromTopic = topicParts[topicParts.length - 1];
                            } else {
                                currentTableFromTopic = null;
                            }

                            // 解析Canal消息，可能包含多个事件
                            List<CdcEvent> events = parseCanalMessage(record.value());

                            for (CdcEvent event : events) {
                                String sourceTable = event.getTable();

                                // 过滤不在配置中的表
                                TableConfigInfo config = tableConfigMap.get(sourceTable);
                                if (config == null) {
                                    log.debug("表不在CDC配置中，跳过: {}", sourceTable);
                                    continue;
                                }

                                // 处理CDC事件
                                processCdcEvent(event, config);

                                successCount.incrementAndGet();
                                processedCount.incrementAndGet();
                            }

                            // 记录待提交位点
                            TopicPartition tp = new TopicPartition(record.topic(), record.partition());
                            pendingOffsets.put(tp, record.offset() + 1);

                            // 定期提交Kafka位点
                            if (shouldCommit()) {
                                synchronized (consumerLock) {
                                    if (kafkaConsumer != null) {
                                        commitKafkaOffsets();
                                        persistPositions(taskId, sourceDsId);
                                    }
                                }
                            }

                        } catch (Exception e) {
                            log.error("处理CDC消息失败: taskId={}, message={}", taskId, record.value(), e);
                            failCount.incrementAndGet();
                        }
                    }

                } catch (Exception e) {
                    log.error("Kafka消费异常: taskId={}", taskId, e);
                    if (!stopped.get()) {
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException ignored) {
                            break;
                        }
                    }
                }
            }
            log.info("CDC事件处理线程结束: taskId={}", taskId);
        });
    }

    /**
     * 处理CDC事件
     * 增加表不存在时的自动恢复机制
     */
    private void processCdcEvent(CdcEvent event, TableConfigInfo config) throws Exception {
        String targetTable = config.targetTable;

        // 检查是否需要自动建表
        if (config.createTargetTable && !createdTables.contains(targetTable)) {
            ensureTargetTableExists(config.sourceTable, targetTable);
        }

        DatabaseConnector targetConnector = null;
        try {
            targetConnector = datasourceService.getConnector(targetDsId);

            // 根据事件类型执行不同操作
            switch (event.getEventType()) {
                case INSERT -> executeInsert(targetConnector, targetTable, event.getAfterData(), event.getMysqlTypes());
                case UPDATE -> executeUpdate(targetConnector, targetTable, event.getAfterData(), event.getPrimaryKeys(), event.getMysqlTypes());
                case DELETE -> executeDelete(targetConnector, targetTable, event.getPrimaryKeys(), event.getMysqlTypes());
            }

            log.debug("CDC事件处理完成: table={} -> {}, type={}",
                event.getTable(), targetTable, event.getEventType());
        } catch (Exception e) {
            String errorMsg = e.getMessage();
            // 如果是表不存在的错误，尝试自动恢复
            if (errorMsg != null && errorMsg.contains("doesn't exist")) {
                log.warn("目标表不存在，尝试重新创建: {}, error={}", targetTable, errorMsg);
                createdTables.remove(targetTable);
                invalidateTableCache(targetTable);
                if (config.createTargetTable) {
                    try {
                        ensureTargetTableExists(config.sourceTable, targetTable);
                        // 重试执行操作
                        targetConnector = datasourceService.getConnector(targetDsId);
                        switch (event.getEventType()) {
                            case INSERT -> executeInsert(targetConnector, targetTable, event.getAfterData(), event.getMysqlTypes());
                            case UPDATE -> executeUpdate(targetConnector, targetTable, event.getAfterData(), event.getPrimaryKeys(), event.getMysqlTypes());
                            case DELETE -> executeDelete(targetConnector, targetTable, event.getPrimaryKeys(), event.getMysqlTypes());
                        }
                        log.info("重试CDC事件处理成功: table={} -> {}, type={}",
                            event.getTable(), targetTable, event.getEventType());
                        return;
                    } catch (Exception retryEx) {
                        log.error("重试CDC事件处理失败: table={} -> {}, error={}",
                            event.getTable(), targetTable, retryEx.getMessage());
                        throw retryEx;
                    }
                }
            }
            log.error("CDC事件处理失败: table={} -> {}, type={}, error={}",
                event.getTable(), targetTable, event.getEventType(), e.getMessage());
            throw e;
        }
    }

    /**
     * 确保目标表存在，不存在则自动创建
     * 增加真实验证，避免缓存导致的误判
     */
    private synchronized void ensureTargetTableExists(String sourceTable, String targetTable) {
        // 双重检查，避免并发重复创建
        if (createdTables.contains(targetTable)) {
            return;
        }

        try {
            DatabaseConnector targetConnector = datasourceService.getConnector(targetDsId);

            // 检查目标表是否已存在（通过实际查询验证，不依赖缓存）
            if (verifyTableExists(targetConnector, targetTable)) {
                log.info("目标表已存在: {}", targetTable);
                createdTables.add(targetTable);
                return;
            }

            // 清除可能存在的过期缓存
            invalidateTableCache(targetTable);

            // 创建目标表
            log.info("自动创建目标表: {} -> {}", sourceTable, targetTable);
            schemaService.createTargetTable(sourceDsId, targetDsId, sourceTable, targetTable, null);
            createdTables.add(targetTable);
            log.info("目标表创建成功: {}", targetTable);

        } catch (Exception e) {
            log.error("创建目标表失败: {} -> {}, error={}", sourceTable, targetTable, e.getMessage(), e);
            // 创建失败时，确保从缓存中移除
            createdTables.remove(targetTable);
            throw new RuntimeException("创建目标表失败: " + targetTable, e);
        }
    }

    /**
     * 通过实际查询验证表是否存在（不依赖元数据缓存）
     */
    private boolean verifyTableExists(DatabaseConnector connector, String tableName) {
        try {
            // 使用getRowCount进行实际查询，如果表不存在会抛出异常
            connector.getRowCount(tableName);
            return true;
        } catch (Exception e) {
            log.debug("表不存在或无法访问: {}, error={}", tableName, e.getMessage());
            return false;
        }
    }

    /**
     * 清除表的元数据缓存
     */
    private void invalidateTableCache(String tableName) {
        try {
            DatabaseConnector targetConnector = datasourceService.getConnector(targetDsId);
            if (targetConnector instanceof com.etl.datasource.connector.AbstractConnector) {
                var manager = ((com.etl.datasource.connector.AbstractConnector) targetConnector).getMetadataCacheManager();
                if (manager != null) {
                    manager.invalidateTable(targetDsId, targetConnector.getDatabaseName(), tableName);
                    log.debug("已清除表缓存: {}", tableName);
                }
            }
        } catch (Exception e) {
            log.debug("清除表缓存失败: {}", e.getMessage());
        }
    }

    /**
     * 判断是否应该提交位点
     */
    private boolean shouldCommit() {
        long now = System.currentTimeMillis();
        long processed = processedCount.get();

        // 按数量间隔
        if (processed % COMMIT_INTERVAL == 0 && processed > 0) {
            return true;
        }
        // 按时间间隔
        return now - lastCommitTime >= COMMIT_TIMEOUT_MS;
    }

    /**
     * 提交Kafka位点 - 必须在持有consumerLock时调用
     */
    private void commitKafkaOffsets() {
        try {
            if (kafkaConsumer != null && !pendingOffsets.isEmpty()) {
                kafkaConsumer.commitSync();
                committedOffsets.putAll(pendingOffsets);
                pendingOffsets.clear();
                lastCommitTime = System.currentTimeMillis();
                log.debug("Kafka位点已提交: taskId={}", taskId);
            }
        } catch (Exception e) {
            log.error("提交Kafka位点失败: taskId={}", taskId, e);
        }
    }

    /**
     * 启动位点持久化线程
     */
    private void startPositionPersistThread(Long taskId, Long sourceDsId) {
        executorService.submit(() -> {
            while (!stopped.get()) {
                try {
                    Thread.sleep(10000); // 每10秒持久化一次
                    persistPositions(taskId, sourceDsId);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }

    /**
     * 持久化位点到数据库
     */
    private synchronized void persistPositions(Long taskId, Long sourceDsId) {
        try {
            for (Map.Entry<TopicPartition, Long> entry : committedOffsets.entrySet()) {
                TopicPartition tp = entry.getKey();
                long offset = entry.getValue();

                String positionValue = tp.partition() + ":" + offset;

                // 查询是否已存在
                EtlCdcPosition existing = cdcPositionMapper.selectOne(
                    new LambdaQueryWrapper<EtlCdcPosition>()
                        .eq(EtlCdcPosition::getTaskId, taskId)
                        .eq(EtlCdcPosition::getTableName, tp.topic()));

                if (existing != null) {
                    existing.setPositionValue(positionValue);
                    existing.setPositionType("KAFKA_OFFSET");
                    cdcPositionMapper.updateById(existing);
                } else {
                    EtlCdcPosition newPos = new EtlCdcPosition();
                    newPos.setTaskId(taskId);
                    newPos.setSourceDsId(sourceDsId);
                    newPos.setTableName(tp.topic());
                    newPos.setPositionType("KAFKA_OFFSET");
                    newPos.setPositionValue(positionValue);
                    cdcPositionMapper.insert(newPos);
                }
            }
            log.debug("位点已持久化到数据库");
        } catch (Exception e) {
            log.error("持久化位点失败", e);
        }
    }

    /**
     * 解析Kafka消息 - 自动识别Debezium和Canal格式
     */
    private List<CdcEvent> parseCanalMessage(String message) {
        List<CdcEvent> events = new ArrayList<>();

        if (StrUtil.isBlank(message)) {
            return events;
        }

        try {
            JSONObject json = JsonUtil.parseObject(message);

            // 判断消息格式
            if (json.containsKey("payload")) {
                // Debezium嵌套格式
                return parseDebeziumMessage(json);
            } else if (json.containsKey("__op") || json.containsKey("__ts_ms")) {
                // Debezium扁平格式（ExtractNewRecordState转换后）
                // 表名从当前处理的记录主题中获取
                return parseDebeziumFlatMessage(json);
            } else {
                // Canal格式（向后兼容）
                return parseCanalFormatMessage(json);
            }
        } catch (Exception e) {
            log.error("解析CDC消息失败: {}", message, e);
        }

        return events;
    }

    /**
     * 当前处理的主题对应的表名（用于Debezium扁平格式）
     */
    private String currentTableFromTopic;

    /**
     * 解析Debezium扁平格式消息（ExtractNewRecordState转换后）
     */
    private List<CdcEvent> parseDebeziumFlatMessage(JSONObject json) {
        List<CdcEvent> events = new ArrayList<>();

        try {
            // 从 __op 字段获取操作类型
            String op = json.getString("__op");
            long timestamp = json.getLongValue("__ts_ms");

            // 解析事件类型
            CdcEventType eventType = parseDebeziumEventType(op);
            if (eventType == null) {
                log.debug("跳过不支持的Debezium扁平事件类型: {}", op);
                return events;
            }

            // 表名从主题中提取（格式: prefix.database.table）
            // currentTableFromTopic 由poll时设置
            String table = currentTableFromTopic;
            if (table == null) {
                log.warn("无法确定表名，跳过消息");
                return events;
            }

            CdcEvent event = new CdcEvent();
            event.setTable(table);
            event.setEventType(eventType);
            event.setTimestamp(timestamp);

            // 检查是否为删除操作
            boolean isDeleted = json.getBooleanValue("__deleted");
            if (isDeleted) {
                event.setEventType(CdcEventType.DELETE);
            }

            // 解析数据（移除元数据字段）
            Map<String, Object> data = new HashMap<>();
            for (String key : json.keySet()) {
                if (!key.startsWith("__")) {
                    data.put(key, json.get(key));
                }
            }
            event.setAfterData(data);

            // 提取主键（假设id字段为主键）
            Map<String, Object> primaryKeys = new HashMap<>();
            if (data.containsKey("id")) {
                primaryKeys.put("id", data.get("id"));
            } else if (data.containsKey("ID")) {
                primaryKeys.put("ID", data.get("ID"));
            }
            event.setPrimaryKeys(primaryKeys);

            events.add(event);
            log.debug("解析Debezium扁平消息: table={}, op={}, dataCount={}", table, op, data.size());

        } catch (Exception e) {
            log.error("解析Debezium扁平消息失败: {}", json.toString(), e);
        }

        return events;
    }

    /**
     * 解析Debezium格式消息
     */
    @SuppressWarnings("unchecked")
    private List<CdcEvent> parseDebeziumMessage(JSONObject json) {
        List<CdcEvent> events = new ArrayList<>();

        try {
            JSONObject payload = json.getJSONObject("payload");
            if (payload == null) {
                return events;
            }

            // 解析source信息
            JSONObject source = payload.getJSONObject("source");
            if (source == null) {
                return events;
            }

            String database = source.getString("db");
            String table = source.getString("table");
            long timestamp = payload.getLongValue("ts_ms");
            String op = payload.getString("op");

            // 解析事件类型
            CdcEventType eventType = parseDebeziumEventType(op);
            if (eventType == null) {
                log.debug("跳过不支持的Debezium事件类型: {}", op);
                return events;
            }

            CdcEvent event = new CdcEvent();
            event.setDatabase(database);
            event.setTable(table);
            event.setEventType(eventType);
            event.setTimestamp(timestamp);

            // 解析变更前数据
            JSONObject before = payload.getJSONObject("before");
            if (before != null) {
                event.setBeforeData(new HashMap<>(before));
            }

            // 解析变更后数据
            JSONObject after = payload.getJSONObject("after");
            if (after != null) {
                event.setAfterData(new HashMap<>(after));
            }

            // 解析主键 - 从after或before中提取
            Map<String, Object> primaryKeys = extractPrimaryKeysFromDebezium(source, after, before);
            event.setPrimaryKeys(primaryKeys);

            // 设置位点信息
            Map<String, Object> sourceOffset = (Map<String, Object>) json.get("sourceOffset");
            if (sourceOffset != null) {
                event.setPosition(String.valueOf(sourceOffset.get("file")) + ":" + sourceOffset.get("pos"));
            }

            events.add(event);
            log.debug("解析Debezium消息: table={}, op={}, count={}", table, op, events.size());

        } catch (Exception e) {
            log.error("解析Debezium消息失败: {}", json.toString(), e);
        }

        return events;
    }

    /**
     * 从Debezium消息中提取主键
     */
    private Map<String, Object> extractPrimaryKeysFromDebezium(JSONObject source, JSONObject after, JSONObject before) {
        Map<String, Object> primaryKeys = new HashMap<>();

        // 尝试从source中获取主键列信息
        JSONArray pkNames = source.getJSONArray("pkNames");
        if (pkNames != null && !pkNames.isEmpty()) {
            for (int i = 0; i < pkNames.size(); i++) {
                String pkName = pkNames.getString(i);
                Object pkValue = after != null ? after.get(pkName) : (before != null ? before.get(pkName) : null);
                if (pkValue != null) {
                    primaryKeys.put(pkName, pkValue);
                }
            }
        }

        // 如果没有主键信息，尝试从数据中推断
        if (primaryKeys.isEmpty()) {
            JSONObject dataToCheck = after != null ? after : before;
            if (dataToCheck != null) {
                // 常见主键列名
                String[] commonPkNames = {"id", "ID", "pk", "PK", "primary_key"};
                for (String pkName : commonPkNames) {
                    if (dataToCheck.containsKey(pkName)) {
                        primaryKeys.put(pkName, dataToCheck.get(pkName));
                        break;
                    }
                }
            }
        }

        return primaryKeys;
    }

    /**
     * 解析Debezium操作类型
     */
    private CdcEventType parseDebeziumEventType(String op) {
        if (op == null) return null;
        return switch (op.toLowerCase()) {
            case "c", "r" -> CdcEventType.INSERT;  // c=create, r=read(snapshot)
            case "u" -> CdcEventType.UPDATE;
            case "d" -> CdcEventType.DELETE;
            default -> null;
        };
    }

    /**
     * 解析Canal格式消息（向后兼容）
     */
    private List<CdcEvent> parseCanalFormatMessage(JSONObject json) {
        List<CdcEvent> events = new ArrayList<>();

        try {
            // 跳过DDL语句
            if (json.getBooleanValue("isDdl")) {
                log.debug("跳过DDL语句: {}", json.getString("sql"));
                return events;
            }

            // 解析通用字段
            String database = json.getString("database");
            String table = json.getString("table");
            long timestamp = json.getLongValue("ts");
            String type = json.getString("type");

            // 解析事件类型
            CdcEventType eventType = parseEventType(type);
            if (eventType == null) {
                log.debug("跳过不支持的事件类型: {}", type);
                return events;
            }

            // 解析主键列名
            JSONArray pkNamesArray = json.getJSONArray("pkNames");
            List<String> pkNames = new ArrayList<>();
            if (pkNamesArray != null) {
                for (int i = 0; i < pkNamesArray.size(); i++) {
                    pkNames.add(pkNamesArray.getString(i));
                }
            }

            // 解析数据数组
            JSONArray dataArray = json.getJSONArray("data");
            JSONArray oldArray = json.getJSONArray("old");

            if (dataArray == null || dataArray.isEmpty()) {
                return events;
            }

            // 为每条数据创建事件
            for (int i = 0; i < dataArray.size(); i++) {
                CdcEvent event = new CdcEvent();
                event.setDatabase(database);
                event.setTable(table);
                event.setEventType(eventType);
                event.setTimestamp(timestamp);

                // 解析mysqlType映射
                JSONObject mysqlTypeObj = json.getJSONObject("mysqlType");
                if (mysqlTypeObj != null) {
                    Map<String, String> mysqlTypes = new HashMap<>();
                    for (String key : mysqlTypeObj.keySet()) {
                        mysqlTypes.put(key, mysqlTypeObj.getString(key));
                    }
                    event.setMysqlTypes(mysqlTypes);
                }

                // 解析变更后的数据
                JSONObject dataObj = dataArray.getJSONObject(i);
                if (dataObj != null) {
                    event.setAfterData(new HashMap<>(dataObj));
                }

                // 解析变更前的数据（UPDATE时）
                if (oldArray != null && i < oldArray.size()) {
                    JSONObject oldObj = oldArray.getJSONObject(i);
                    if (oldObj != null) {
                        event.setBeforeData(new HashMap<>(oldObj));
                    }
                }

                // 解析主键值
                // 如果pkNames为空，默认假设id列是主键
                List<String> effectivePkNames = pkNames;
                if (effectivePkNames.isEmpty() && event.getAfterData() != null) {
                    if (event.getAfterData().containsKey("id")) {
                        effectivePkNames = Collections.singletonList("id");
                        log.debug("pkNames为空，默认使用id作为主键: table={}", table);
                    }
                }

                if (!effectivePkNames.isEmpty()) {
                    Map<String, Object> primaryKeys = new HashMap<>();
                    for (String pkName : effectivePkNames) {
                        // 先从变更后的数据获取主键
                        Object pkValue = event.getAfterData() != null ? event.getAfterData().get(pkName) : null;
                        // 如果是DELETE事件，从变更前数据获取
                        if (pkValue == null && event.getBeforeData() != null) {
                            pkValue = event.getBeforeData().get(pkName);
                        }
                        if (pkValue != null) {
                            primaryKeys.put(pkName, pkValue);
                        }
                    }
                    event.setPrimaryKeys(primaryKeys);
                }

                // 设置位点信息
                event.setPosition(String.valueOf(json.getLongValue("id")));

                events.add(event);
            }

            log.debug("解析Canal消息: table={}, type={}, count={}", table, type, events.size());

        } catch (Exception e) {
            log.error("解析Canal消息失败: {}", json.toString(), e);
        }

        return events;
    }

    /**
     * 解析事件类型
     */
    private CdcEventType parseEventType(String type) {
        if (type == null) return null;
        return switch (type.toUpperCase()) {
            case "INSERT" -> CdcEventType.INSERT;
            case "UPDATE" -> CdcEventType.UPDATE;
            case "DELETE" -> CdcEventType.DELETE;
            default -> null;
        };
    }

    /**
     * 执行INSERT操作 - 支持幂等性（重复插入不会报错）
     */
    private void executeInsert(DatabaseConnector connector, String tableName, Map<String, Object> data,
                               Map<String, String> mysqlTypes) throws Exception {
        if (data == null || data.isEmpty()) {
            log.warn("INSERT数据为空，跳过: {}", tableName);
            return;
        }

        String quote = connector.getDatabaseType().equals("POSTGRESQL") ? "\"" : "`";
        String dbType = connector.getDatabaseType();
        StringBuilder sql = new StringBuilder();

        List<String> columns = new ArrayList<>(data.keySet());

        if ("MYSQL".equals(dbType)) {
            // MySQL使用 INSERT IGNORE 处理重复键
            sql.append("INSERT IGNORE INTO ");
        } else if ("DORIS".equals(dbType)) {
            // Doris使用 INSERT
            sql.append("INSERT INTO ");
        } else if ("POSTGRESQL".equals(dbType)) {
            // PostgreSQL使用 ON CONFLICT DO NOTHING
            sql.append("INSERT INTO ");
        } else {
            sql.append("INSERT INTO ");
        }

        sql.append(quote).append(tableName).append(quote).append(" (");
        sql.append(String.join(", ", columns.stream().map(c -> quote + c + quote).toList()));
        sql.append(") VALUES (");
        sql.append(String.join(", ", Collections.nCopies(columns.size(), "?")));
        sql.append(")");

        // PostgreSQL需要添加ON CONFLICT子句
        if ("POSTGRESQL".equals(dbType)) {
            sql.append(" ON CONFLICT DO NOTHING");
        }

        log.debug("执行INSERT: table={}, columns={}", tableName, columns);

        Connection conn = null;
        try {
            conn = connector.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
                for (int i = 0; i < columns.size(); i++) {
                    String columnName = columns.get(i);
                    String mysqlType = mysqlTypes != null ? mysqlTypes.get(columnName) : null;
                    Object value = convertValue(data.get(columnName), columnName, mysqlType);
                    stmt.setObject(i + 1, value);
                }
                int rows = stmt.executeUpdate();
                log.debug("INSERT完成: table={}, rows={}", tableName, rows);
            }
        } finally {
            // 确保连接关闭，避免连接池耗尽
            if (conn != null) {
                try {
                    conn.close();
                } catch (Exception ignored) {}
            }
        }
    }

    /**
     * 执行UPDATE操作
     */
    private void executeUpdate(DatabaseConnector connector, String tableName,
                                Map<String, Object> data, Map<String, Object> primaryKeys,
                                Map<String, String> mysqlTypes) throws Exception {
        if (data == null || data.isEmpty()) {
            log.warn("UPDATE数据为空，跳过: {}", tableName);
            return;
        }

        String quote = connector.getDatabaseType().equals("POSTGRESQL") ? "\"" : "`";
        StringBuilder sql = new StringBuilder();
        sql.append("UPDATE ").append(quote).append(tableName).append(quote).append(" SET ");

        List<String> setColumns = new ArrayList<>(data.keySet());
        sql.append(String.join(", ", setColumns.stream().map(c -> quote + c + quote + " = ?").toList()));

        if (primaryKeys != null && !primaryKeys.isEmpty()) {
            sql.append(" WHERE ");
            List<String> pkColumns = new ArrayList<>(primaryKeys.keySet());
            sql.append(String.join(" AND ", pkColumns.stream().map(c -> quote + c + quote + " = ?").toList()));
        }

        log.debug("执行UPDATE: table={}, pk={}", tableName, primaryKeys);

        Connection conn = null;
        try {
            conn = connector.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
                int idx = 1;
                for (String column : setColumns) {
                    String mysqlType = mysqlTypes != null ? mysqlTypes.get(column) : null;
                    Object value = convertValue(data.get(column), column, mysqlType);
                    stmt.setObject(idx++, value);
                }
                if (primaryKeys != null) {
                    for (String pk : primaryKeys.keySet()) {
                        String mysqlType = mysqlTypes != null ? mysqlTypes.get(pk) : null;
                        Object value = convertValue(primaryKeys.get(pk), pk, mysqlType);
                        stmt.setObject(idx++, value);
                    }
                }
                int rows = stmt.executeUpdate();
                log.debug("UPDATE完成: table={}, rows={}", tableName, rows);
            }
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (Exception ignored) {}
            }
        }
    }

    /**
     * 执行DELETE操作
     */
    private void executeDelete(DatabaseConnector connector, String tableName, Map<String, Object> primaryKeys,
                                Map<String, String> mysqlTypes) throws Exception {
        if (primaryKeys == null || primaryKeys.isEmpty()) {
            log.warn("DELETE主键为空，跳过: {}", tableName);
            return;
        }

        String quote = connector.getDatabaseType().equals("POSTGRESQL") ? "\"" : "`";
        StringBuilder sql = new StringBuilder();
        sql.append("DELETE FROM ").append(quote).append(tableName).append(quote).append(" WHERE ");

        List<String> pkColumns = new ArrayList<>(primaryKeys.keySet());
        sql.append(String.join(" AND ", pkColumns.stream().map(c -> quote + c + quote + " = ?").toList()));

        log.debug("执行DELETE: table={}, pk={}", tableName, primaryKeys);

        Connection conn = null;
        try {
            conn = connector.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
                int idx = 1;
                for (String pk : pkColumns) {
                    String mysqlType = mysqlTypes != null ? mysqlTypes.get(pk) : null;
                    Object value = convertValue(primaryKeys.get(pk), pk, mysqlType);
                    stmt.setObject(idx++, value);
                }
                int rows = stmt.executeUpdate();
                log.debug("DELETE完成: table={}, rows={}", tableName, rows);
            }
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (Exception ignored) {}
            }
        }
    }

    /**
     * 转换值为适当的Java类型，以匹配目标数据库
     * 根据Canal消息中的mysqlType信息进行类型转换
     */
    private Object convertValue(Object value, String columnName, String mysqlType) {
        if (value == null) {
            return null;
        }

        // 处理Number类型的时间戳（Debezium发送毫秒级Unix时间戳或天数）
        if (value instanceof Number) {
            Number numValue = (Number) value;
            long longValue = numValue.longValue();

            if (mysqlType != null) {
                String upperType = mysqlType.toUpperCase();
                // DATE类型：Debezium发送自纪元以来的天数
                if (upperType.contains("DATE") && !upperType.contains("DATETIME") && !upperType.contains("TIMESTAMP")) {
                    try {
                        LocalDate localDate = LocalDate.ofEpochDay(longValue);
                        return java.sql.Date.valueOf(localDate);
                    } catch (Exception e) {
                        log.debug("DATE转换失败(天数): {} = {}", columnName, longValue);
                    }
                }
                if (upperType.contains("DATETIME") || upperType.contains("TIMESTAMP")) {
                    return new Timestamp(longValue);
                }
            } else {
                // 没有mysqlType时，根据字段名智能判断
                String lowerName = columnName.toLowerCase();
                // 日期字段（非时间戳）：值较小，可能是天数
                if (lowerName.contains("date") && !lowerName.contains("time") && longValue > 0 && longValue < 100000) {
                    try {
                        LocalDate localDate = LocalDate.ofEpochDay(longValue);
                        return java.sql.Date.valueOf(localDate);
                    } catch (Exception e) {
                        log.debug("DATE推断转换失败: {} = {}", columnName, longValue);
                    }
                }
                // 时间戳字段：值较大，可能是毫秒
                if (isLikelyTimestamp(columnName, longValue)) {
                    return new Timestamp(longValue);
                }
            }
        }

        // 如果不是字符串，直接返回
        if (!(value instanceof String)) {
            return value;
        }

        String strValue = (String) value;
        if (strValue.isEmpty()) {
            if (mysqlType != null) {
                String upperType = mysqlType.toUpperCase();
                if (upperType.contains("VARCHAR") || upperType.contains("CHAR") ||
                    upperType.contains("TEXT") || upperType.contains("LONGTEXT")) {
                    return "";
                }
                return null;
            }
            return "";
        }

        // 根据mysqlType进行类型转换
        if (mysqlType != null) {
            String upperType = mysqlType.toUpperCase();

            // 处理整数类型
            if (upperType.contains("BIGINT") || upperType.contains("LONG")) {
                try {
                    return Long.parseLong(strValue);
                } catch (NumberFormatException e) {
                    log.debug("BIGINT转换失败: {} = {}", columnName, strValue);
                }
            } else if (upperType.contains("INT") || upperType.contains("TINYINT") ||
                       upperType.contains("SMALLINT") || upperType.contains("MEDIUMINT")) {
                try {
                    return Integer.parseInt(strValue);
                } catch (NumberFormatException e) {
                    log.debug("INT转换失败: {} = {}", columnName, strValue);
                }
            } else if (upperType.contains("DECIMAL") || upperType.contains("NUMERIC") ||
                       upperType.contains("DOUBLE") || upperType.contains("FLOAT")) {
                try {
                    return new java.math.BigDecimal(strValue);
                } catch (NumberFormatException e) {
                    log.debug("DECIMAL转换失败: {} = {}", columnName, strValue);
                }
            } else if (upperType.contains("DATETIME") || upperType.contains("TIMESTAMP")) {
                // 处理日期时间类型
                try {
                    // 支持多种日期时间格式
                    String cleanValue = strValue.trim();

                    // ISO 8601 格式 (Debezium): 2026-04-16T13:32:26Z 或 2026-04-16T13:32:26.123Z
                    if (cleanValue.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*Z?")) {
                        // 移除 T 和 Z，转换为标准格式
                        cleanValue = cleanValue.replace("T", " ").replace("Z", "");
                        // 截取到秒
                        cleanValue = cleanValue.substring(0, Math.min(19, cleanValue.length()));
                        LocalDateTime ldt = LocalDateTime.parse(cleanValue,
                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                        return Timestamp.valueOf(ldt);
                    }

                    // 标准格式: 2026-04-16 13:32:26
                    cleanValue = cleanValue.substring(0, Math.min(19, cleanValue.length()));
                    LocalDateTime ldt = LocalDateTime.parse(cleanValue,
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    return Timestamp.valueOf(ldt);
                } catch (Exception e) {
                    log.debug("DATETIME转换失败: {} = {}", columnName, strValue);
                }
            } else if (upperType.contains("DATE")) {
                // 处理日期类型
                try {
                    LocalDateTime ldt = LocalDateTime.parse(strValue + " 00:00:00",
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    return Timestamp.valueOf(ldt);
                } catch (Exception e) {
                    log.debug("DATE转换失败: {} = {}", columnName, strValue);
                }
            }
        }

        // 智能检测日期时间格式（向后兼容）
        // ISO 8601 格式 (Debezium): 2026-04-16T13:32:26Z 或 2026-04-16T13:32:26.123456Z
        if (strValue.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*Z?")) {
            try {
                String cleanValue = strValue.replace("T", " ").replace("Z", "");
                cleanValue = cleanValue.substring(0, Math.min(19, cleanValue.length()));
                LocalDateTime ldt = LocalDateTime.parse(cleanValue,
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                return Timestamp.valueOf(ldt);
            } catch (Exception e) {
                log.debug("ISO 8601日期时间解析失败，保留原值: {} = {}", columnName, strValue);
            }
        } else if (strValue.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}.*")) {
            try {
                String cleanValue = strValue.substring(0, Math.min(19, strValue.length()));
                LocalDateTime ldt = LocalDateTime.parse(cleanValue,
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                return Timestamp.valueOf(ldt);
            } catch (Exception e) {
                log.debug("日期时间解析失败，保留原值: {} = {}", columnName, strValue);
            }
        } else if (strValue.matches("\\d{4}-\\d{2}-\\d{2}")) {
            try {
                LocalDateTime ldt = LocalDateTime.parse(strValue + " 00:00:00",
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                return Timestamp.valueOf(ldt);
            } catch (Exception e) {
                log.debug("日期解析失败，保留原值: {} = {}", columnName, strValue);
            }
        }

        return value;
    }

    /**
     * 判断字段名和值是否可能是毫秒级时间戳
     */
    private boolean isLikelyTimestamp(String columnName, long value) {
        if (columnName == null) {
            return false;
        }
        String lowerName = columnName.toLowerCase();
        boolean isTimeField = lowerName.contains("time") || lowerName.contains("date")
            || lowerName.endsWith("_at") || lowerName.endsWith("time")
            || lowerName.endsWith("date") || lowerName.startsWith("time_")
            || lowerName.startsWith("date_");
        if (!isTimeField) {
            return false;
        }
        // 合理的毫秒时间戳范围（2000-01-01 ~ 2100-01-01）
        return value > 946684800000L && value < 4102444800000L;
    }

    /**
     * 重载方法，不传入mysqlType（向后兼容）
     */
    private Object convertValue(Object value, String columnName) {
        return convertValue(value, columnName, null);
    }

    private void shutdown() {
        // 确保只执行一次
        if (!shutdownCalled.compareAndSet(false, true)) {
            log.info("shutdown已执行，跳过重复调用");
            return;
        }

        stopped.set(true);
        log.info("开始执行CDC引擎shutdown: taskId={}", taskId);

        // 最终提交位点
        synchronized (consumerLock) {
            try {
                if (kafkaConsumer != null) {
                    commitKafkaOffsets();
                }
            } catch (Exception e) {
                log.error("最终提交位点失败", e);
            }

            if (kafkaConsumer != null) {
                try {
                    kafkaConsumer.close(Duration.ofSeconds(10));
                    log.info("Kafka消费者已关闭: taskId={}", taskId);
                } catch (Exception e) {
                    log.error("关闭Kafka消费者失败: taskId={}", taskId, e);
                }
                kafkaConsumer = null;
            }
        }

        if (executorService != null) {
            try {
                executorService.shutdown();
                if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
                log.info("线程池已关闭: taskId={}", taskId);
            } catch (Exception e) {
                log.error("关闭线程池失败: taskId={}", taskId, e);
            }
        }
    }

    @Override
    public void stop() {
        log.info("收到停止信号: taskId={}", taskId);
        stopped.set(true);
        shutdown();
    }

    @Override
    public int getProgress() {
        return progress;
    }

    /**
     * 获取统计信息
     */
    public Map<String, Long> getStatistics() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("processed", processedCount.get());
        stats.put("success", successCount.get());
        stats.put("failed", failCount.get());
        return stats;
    }
}
