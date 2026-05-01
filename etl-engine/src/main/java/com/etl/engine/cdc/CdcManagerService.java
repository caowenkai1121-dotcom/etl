package com.etl.engine.cdc;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.etl.common.enums.TaskStatus;
import com.etl.common.enums.TriggerType;
import com.etl.common.utils.JsonUtil;
import com.etl.datasource.connector.DatabaseConnector;
import com.etl.datasource.service.DatasourceService;
import com.etl.engine.entity.EtlCdcConfig;
import com.etl.engine.entity.EtlSyncTask;
import com.etl.engine.schema.TypeMappingService;
import com.etl.engine.entity.EtlTaskExecution;
import com.etl.engine.mapper.CdcPositionMapper;
import com.etl.engine.service.CdcConfigService;
import com.etl.engine.service.SyncTaskService;
import com.etl.engine.service.TaskExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * CDC任务管理服务
 * Debezium Kafka Connect模式 - 通过Debezium监听数据库变更发送到Kafka，ETL系统消费Kafka消息
 *
 * 架构: MySQL/PostgreSQL -> Debezium Connect -> Kafka -> ETL系统
 *
 * 支持多数据源CDC配置：
 * - 每个数据源可以配置独立的Debezium连接器
 * - 每个连接器发送到独立的Kafka Topic
 * - CDC任务根据关联的数据源确定消费哪个Topic
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CdcManagerService {

    private final SyncTaskService syncTaskService;
    private final TaskExecutionService taskExecutionService;
    private final DatasourceService datasourceService;
    private final CdcPositionMapper cdcPositionMapper;
    private final CdcConfigService cdcConfigService;
    private final DebeziumConnectorManager debeziumConnectorManager;
    private final TypeMappingService typeMappingService;

    @Value("${cdc.kafka.bootstrap-servers:kafka:9092}")
    private String kafkaBootstrapServers;

    @Value("${cdc.kafka.default-topic:etl-cdc}")
    private String defaultKafkaTopic;

    // 正在运行的CDC任务
    private final Map<Long, CdcRunningTask> runningCdcTasks = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    /**
     * 根据数据源ID获取Kafka topic名称
     * 规则:
     * 1. 查询该数据源对应的CDC配置
     * 2. 如果CDC配置中有指定kafkaTopicPrefix，使用该前缀
     * 3. 否则使用serverName作为前缀
     *
     * 注意: Debezium的Topic格式为 {prefix}.{database}.{table}
     */
    private String getTopicByDatasourceId(Long datasourceId) {
        // 从数据库查询CDC配置
        EtlCdcConfig cdcConfig = cdcConfigService.getByDatasourceId(datasourceId);
        if (cdcConfig != null) {
            if (StrUtil.isNotBlank(cdcConfig.getKafkaTopicPrefix())) {
                log.debug("数据源 {} 使用自定义Kafka Topic前缀: {}", datasourceId, cdcConfig.getKafkaTopicPrefix());
                return cdcConfig.getKafkaTopicPrefix();
            }
            if (StrUtil.isNotBlank(cdcConfig.getServerName())) {
                log.debug("数据源 {} 使用serverName作为Topic前缀: {}", datasourceId, cdcConfig.getServerName());
                return cdcConfig.getServerName();
            }
        }
        // 使用默认topic
        return defaultKafkaTopic;
    }

    /**
     * 获取数据源对应的CDC配置
     */
    public EtlCdcConfig getCdcConfigByDatasourceId(Long datasourceId) {
        return cdcConfigService.getByDatasourceId(datasourceId);
    }

    /**
     * 启动CDC任务
     */
    public void startCdcTask(Long taskId) {
        if (runningCdcTasks.containsKey(taskId)) {
            log.warn("CDC任务已在运行: taskId={}", taskId);
            return;
        }

        EtlSyncTask task = syncTaskService.getDetail(taskId);
        if (task == null) {
            throw new RuntimeException("任务不存在: " + taskId);
        }

        // 根据数据源ID确定Kafka topic
        Long sourceDsId = task.getSourceDsId();
        String kafkaTopicPrefix = getTopicByDatasourceId(sourceDsId);

        log.info("=== CDC任务启动 === taskId={}, sourceDsId={}, topicPrefix={}", taskId, sourceDsId, kafkaTopicPrefix);

        // 获取源数据源类型
        DatabaseConnector sourceConnector = datasourceService.getConnector(sourceDsId);
        String sourceDbType = sourceConnector.getDatabaseType();

        // 检查是否支持CDC
        if (!isCdcSupported(sourceDbType)) {
            throw new RuntimeException("CDC不支持数据库类型: " + sourceDbType + "，请使用增量同步模式");
        }

        // 动态更新connector的表配置 - 只同步配置的表
        updateConnectorTableList(sourceDsId, sourceConnector);

        // 获取表配置
        JSONArray tableConfig = JsonUtil.parseArray(task.getTableConfig());
        Map<String, String> tableMapping = parseTableMapping(tableConfig);

        // 创建执行记录
        EtlTaskExecution execution = taskExecutionService.createExecution(taskId, TriggerType.CDC.getCode());

        // 更新任务状态
        syncTaskService.updateStatus(taskId, TaskStatus.RUNNING.getCode());

        // 创建运行任务
        CdcRunningTask runningTask = new CdcRunningTask();
        runningTask.setTaskId(taskId);
        runningTask.setExecutionId(execution.getId());
        runningTask.setStartTime(System.currentTimeMillis());
        runningTask.setSourceDbType(sourceDbType);

        // 启动Kafka消费模式CDC - 使用数据源对应的topic前缀
        startKafkaCdc(task, tableMapping, runningTask, kafkaTopicPrefix);

        runningCdcTasks.put(taskId, runningTask);
        log.info("CDC任务启动成功: taskId={}, mode=Kafka消费(Debezium), topicPrefix={}", taskId, kafkaTopicPrefix);
    }

    /**
     * 检查数据库类型是否支持CDC
     */
    private boolean isCdcSupported(String dbType) {
        if (dbType == null) return false;
        return switch (dbType.toUpperCase()) {
            case "MYSQL", "POSTGRESQL", "POSTGRES" -> true;
            default -> false;
        };
    }

    /**
     * 启动Kafka消费模式CDC
     * @param kafkaTopicPrefix 数据源对应的Kafka topic前缀
     */
    private void startKafkaCdc(EtlSyncTask task, Map<String, String> tableMapping, CdcRunningTask runningTask, String kafkaTopicPrefix) {
        executorService.submit(() -> {
            CdcSyncEngine engine = null;
            try {
                // 创建CDC同步引擎 - 使用数据源对应的topic前缀
                // Debezium的Topic格式: {prefix}.{database}.{table}
                // 需要订阅所有相关的Topic
                engine = new CdcSyncEngine(
                    syncTaskService,
                    taskExecutionService,
                    datasourceService,
                    cdcPositionMapper,
                    kafkaBootstrapServers,
                    kafkaTopicPrefix,
                    typeMappingService,
                    cdcConfigService
                );
                runningTask.setEngine(engine);

                // 创建同步上下文
                var context = new com.etl.common.domain.SyncPipelineContext();
                context.setTaskId(task.getId());

                // 执行同步
                engine.sync(context);

            } catch (Exception e) {
                log.error("CDC同步异常: taskId={}", task.getId(), e);
                // 更新任务状态为失败
                syncTaskService.updateStatus(task.getId(), TaskStatus.STOPPED.getCode());
                taskExecutionService.completeExecution(runningTask.getExecutionId(),
                    com.etl.common.enums.ExecutionStatus.FAILED.getCode(),
                    "CDC同步异常: " + e.getMessage(), null);
            } finally {
                runningTask.setRunning(false);
                runningCdcTasks.remove(task.getId());
                log.info("CDC同步线程结束: taskId={}", task.getId());
            }
        });
    }

    /**
     * 停止CDC任务
     */
    public void stopCdcTask(Long taskId) {
        CdcRunningTask runningTask = runningCdcTasks.remove(taskId);
        if (runningTask == null) {
            log.warn("CDC任务未在运行: taskId={}", taskId);
            return;
        }

        runningTask.setRunning(false);

        if (runningTask.getEngine() != null) {
            runningTask.getEngine().stop();
        }

        // 获取任务信息用于更新表列表
        EtlSyncTask task = syncTaskService.getById(taskId);
        Long sourceDsId = task != null ? task.getSourceDsId() : null;

        syncTaskService.updateStatus(taskId, TaskStatus.STOPPED.getCode());

        taskExecutionService.completeExecution(runningTask.getExecutionId(),
            com.etl.common.enums.ExecutionStatus.SUCCESS.getCode(),
            "CDC任务已停止，处理事件数: " + runningTask.getProcessedCount(), null);

        log.info("CDC任务已停止: taskId={}, processedCount={}", taskId, runningTask.getProcessedCount());

        // 动态更新connector表列表，移除已停止任务的表
        if (sourceDsId != null) {
            try {
                DatabaseConnector sourceConnector = datasourceService.getConnector(sourceDsId);
                updateConnectorTableList(sourceDsId, sourceConnector);
                log.info("已更新connector表列表，移除停止任务的表: taskId={}, sourceDsId={}", taskId, sourceDsId);
            } catch (Exception e) {
                log.warn("更新connector表列表失败: taskId={}, error={}", taskId, e.getMessage());
            }
        }
    }

    /**
     * 部署CDC连接器
     */
    public boolean deployCdcConnector(Long configId) {
        return debeziumConnectorManager.deployConnector(configId);
    }

    /**
     * 删除CDC连接器
     */
    public boolean deleteCdcConnector(String connectorName) {
        return debeziumConnectorManager.deleteConnector(connectorName);
    }

    /**
     * 获取CDC连接器状态
     */
    public JSONObject getCdcConnectorStatus(String connectorName) {
        return debeziumConnectorManager.getConnectorStatus(connectorName);
    }

    public CdcRunningTask getCdcTaskStatus(Long taskId) {
        return runningCdcTasks.get(taskId);
    }

    public boolean isRunning(Long taskId) {
        return runningCdcTasks.containsKey(taskId);
    }

    public Map<Long, CdcRunningTask> getAllRunningTasks() {
        return new HashMap<>(runningCdcTasks);
    }

    private Map<String, String> parseTableMapping(JSONArray tableConfig) {
        Map<String, String> mapping = new HashMap<>();
        if (tableConfig != null) {
            for (int i = 0; i < tableConfig.size(); i++) {
                JSONObject table = tableConfig.getJSONObject(i);
                String sourceTable = table.getString("sourceTable");
                String targetTable = table.getString("targetTable");
                if (StrUtil.isNotBlank(sourceTable)) {
                    mapping.put(sourceTable, StrUtil.isNotBlank(targetTable) ? targetTable : sourceTable);
                }
            }
        }
        return mapping;
    }

    /**
     * 动态更新connector的表列表
     * 收集该数据源下所有CDC任务的表配置，更新Debezium connector只捕获这些表
     */
    private void updateConnectorTableList(Long datasourceId, DatabaseConnector sourceConnector) {
        // 获取数据源对应的CDC配置
        EtlCdcConfig cdcConfig = cdcConfigService.getByDatasourceId(datasourceId);
        if (cdcConfig == null) {
            log.warn("数据源没有CDC配置，跳过表列表更新: datasourceId={}", datasourceId);
            return;
        }

        // 获取该数据源下所有CDC任务
        List<EtlSyncTask> cdcTasks = syncTaskService.getCdcTasksByDatasourceId(datasourceId);
        if (cdcTasks.isEmpty()) {
            log.warn("数据源没有CDC任务，跳过表列表更新: datasourceId={}", datasourceId);
            return;
        }

        // 收集所有需要同步的表（格式: database.table）
        Set<String> tablesToSync = new HashSet<>();
        String databaseName = sourceConnector.getDatabaseName();

        for (EtlSyncTask task : cdcTasks) {
            JSONArray tableConfig = JsonUtil.parseArray(task.getTableConfig());
            if (tableConfig != null) {
                for (int i = 0; i < tableConfig.size(); i++) {
                    JSONObject table = tableConfig.getJSONObject(i);
                    String sourceTable = table.getString("sourceTable");
                    if (StrUtil.isNotBlank(sourceTable)) {
                        // 格式: database.table
                        tablesToSync.add(databaseName + "." + sourceTable);
                    }
                }
            }
        }

        if (tablesToSync.isEmpty()) {
            log.warn("没有需要同步的表，跳过表列表更新: datasourceId={}", datasourceId);
            return;
        }

        // 更新connector配置
        log.info("更新connector表列表: datasourceId={}, tables={}", datasourceId, tablesToSync);
        boolean updated = debeziumConnectorManager.updateConnectorTables(cdcConfig.getId(), tablesToSync);
        if (updated) {
            log.info("Connector表列表更新成功: configId={}, tableCount={}", cdcConfig.getId(), tablesToSync.size());
        } else {
            log.warn("Connector表列表更新失败: configId={}", cdcConfig.getId());
        }
    }

    @lombok.Data
    public static class CdcRunningTask {
        private Long taskId;
        private Long executionId;
        private long startTime;
        private volatile boolean running = true;
        private long processedCount = 0;
        private String sourceDbType;
        private CdcSyncEngine engine;

        public synchronized void incrementProcessedCount() {
            processedCount++;
        }

        public long getDuration() {
            return System.currentTimeMillis() - startTime;
        }
    }
}
