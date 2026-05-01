package com.etl.engine.cdc;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.etl.common.utils.EncryptionUtil;
import com.etl.datasource.entity.EtlDatasource;
import com.etl.datasource.service.DatasourceService;
import com.etl.engine.entity.EtlCdcConfig;
import com.etl.engine.service.CdcConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Debezium连接器管理器
 * 通过Debezium Kafka Connect REST API管理连接器
 *
 * 支持MySQL和PostgreSQL数据库的CDC
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DebeziumConnectorManager {

    private final CdcConfigService cdcConfigService;
    private final DatasourceService datasourceService;

    @Value("${cdc.debezium.connect-url:http://debezium-connect:8083}")
    private String debeziumConnectUrl;

    @Value("${cdc.kafka.bootstrap-servers:kafka:9092}")
    private String kafkaBootstrapServers;

    /**
     * 部署CDC配置到Debezium Connect
     * 创建或更新Debezium连接器
     */
    public boolean deployConnector(Long configId) {
        EtlCdcConfig config = cdcConfigService.getById(configId);
        if (config == null) {
            throw new RuntimeException("CDC配置不存在: " + configId);
        }

        // 获取数据库连接信息
        DatabaseConnectionInfo dbInfo = getDatabaseConnectionInfo(config);

        // 构建Debezium连接器配置（直接是config内容）
        Map<String, String> connectorConfig = buildConnectorConfigDirect(config, dbInfo);

        try {
            String connectorName = config.getConnectorName();
            // 使用PUT /connectors/{name}/config 端点，直接发送config内容
            String url = debeziumConnectUrl + "/connectors/" + connectorName + "/config";
            String jsonBody = JSON.toJSONString(connectorConfig);

            log.info("部署Debezium连接器: url={}, body={}", url, jsonBody);

            HttpResponse response = HttpRequest.put(url)
                .body(jsonBody, "application/json")
                .timeout(30000)
                .execute();

            if (response.isOk()) {
                log.info("部署Debezium连接器成功: connector={}", connectorName);
                cdcConfigService.updateSyncStatus(configId, "RUNNING", null);
                return true;
            } else {
                String errorMsg = "部署Debezium连接器失败: " + response.body();
                log.error(errorMsg);
                cdcConfigService.updateSyncStatus(configId, "ERROR", errorMsg);
                return false;
            }
        } catch (Exception e) {
            String errorMsg = "部署Debezium连接器异常: " + e.getMessage();
            log.error(errorMsg, e);
            cdcConfigService.updateSyncStatus(configId, "ERROR", errorMsg);
            return false;
        }
    }

    /**
     * 启动Debezium连接器（恢复暂停的连接器）
     */
    public boolean startConnector(String connectorName) {
        try {
            String url = debeziumConnectUrl + "/connectors/" + connectorName + "/resume";
            HttpResponse response = HttpRequest.put(url)
                .body("", "application/json")
                .timeout(10000)
                .execute();

            if (response.isOk()) {
                log.info("恢复Debezium连接器成功: connector={}", connectorName);
                return true;
            } else {
                log.error("恢复Debezium连接器失败: connector={}, response={}", connectorName, response.body());
                return false;
            }
        } catch (Exception e) {
            log.error("恢复Debezium连接器异常: connector={}", connectorName, e);
            return false;
        }
    }

    /**
     * 暂停Debezium连接器
     */
    public boolean pauseConnector(String connectorName) {
        try {
            String url = debeziumConnectUrl + "/connectors/" + connectorName + "/pause";
            HttpResponse response = HttpRequest.put(url)
                .body("", "application/json")
                .timeout(10000)
                .execute();

            if (response.isOk()) {
                log.info("暂停Debezium连接器成功: connector={}", connectorName);
                return true;
            } else {
                log.error("暂停Debezium连接器失败: connector={}, response={}", connectorName, response.body());
                return false;
            }
        } catch (Exception e) {
            log.error("暂停Debezium连接器异常: connector={}", connectorName, e);
            return false;
        }
    }

    /**
     * 停止并删除Debezium连接器
     */
    public boolean deleteConnector(String connectorName) {
        try {
            // 先暂停连接器
            pauseConnector(connectorName);

            String url = debeziumConnectUrl + "/connectors/" + connectorName;
            HttpResponse response = HttpRequest.delete(url)
                .contentType("application/json")
                .timeout(10000)
                .execute();

            if (response.isOk()) {
                log.info("删除Debezium连接器成功: connector={}", connectorName);
                return true;
            } else {
                log.error("删除Debezium连接器失败: connector={}, response={}", connectorName, response.body());
                return false;
            }
        } catch (Exception e) {
            log.error("删除Debezium连接器异常: connector={}", connectorName, e);
            return false;
        }
    }

    /**
     * 检查Debezium连接器是否存在
     */
    public boolean checkConnectorExists(String connectorName) {
        try {
            String url = debeziumConnectUrl + "/connectors/" + connectorName;
            HttpResponse response = HttpRequest.get(url)
                .timeout(5000)
                .execute();
            return response.isOk();
        } catch (Exception e) {
            log.warn("检查Debezium连接器异常: connector={}", connectorName, e);
            return false;
        }
    }

    /**
     * 获取Debezium连接器状态
     */
    public JSONObject getConnectorStatus(String connectorName) {
        try {
            String url = debeziumConnectUrl + "/connectors/" + connectorName + "/status";
            HttpResponse response = HttpRequest.get(url)
                .timeout(5000)
                .execute();

            if (response.isOk()) {
                return JSON.parseObject(response.body());
            }
            return null;
        } catch (Exception e) {
            log.error("获取Debezium连接器状态异常: connector={}", connectorName, e);
            return null;
        }
    }

    /**
     * 获取连接器任务状态
     */
    public JSONObject getConnectorTaskStatus(String connectorName) {
        try {
            String url = debeziumConnectUrl + "/connectors/" + connectorName + "/tasks/0/status";
            HttpResponse response = HttpRequest.get(url)
                .timeout(5000)
                .execute();

            if (response.isOk()) {
                return JSON.parseObject(response.body());
            }
            return null;
        } catch (Exception e) {
            log.error("获取Debezium连接器任务状态异常: connector={}", connectorName, e);
            return null;
        }
    }

    /**
     * 重启连接器任务
     */
    public boolean restartConnectorTask(String connectorName, int taskId) {
        try {
            String url = debeziumConnectUrl + "/connectors/" + connectorName + "/tasks/" + taskId + "/restart";
            HttpResponse response = HttpRequest.post(url)
                .timeout(10000)
                .execute();

            if (response.isOk()) {
                log.info("重启Debezium连接器任务成功: connector={}, task={}", connectorName, taskId);
                return true;
            } else {
                log.error("重启Debezium连接器任务失败: connector={}, task={}, response={}",
                    connectorName, taskId, response.body());
                return false;
            }
        } catch (Exception e) {
            log.error("重启Debezium连接器任务异常: connector={}, task={}", connectorName, taskId, e);
            return false;
        }
    }

    /**
     * 更新连接器的表过滤配置
     * 动态更新 table.include.list，只同步需要同步的表
     *
     * @param configId CDC配置ID
     * @param tables 需要同步的表列表（格式: database.table）
     * @return 是否更新成功
     */
    public boolean updateConnectorTables(Long configId, java.util.Set<String> tables) {
        EtlCdcConfig config = cdcConfigService.getById(configId);
        if (config == null) {
            throw new RuntimeException("CDC配置不存在: " + configId);
        }

        if (tables == null || tables.isEmpty()) {
            log.warn("表列表为空，跳过更新: configId={}", configId);
            return false;
        }

        String connectorName = config.getConnectorName();

        try {
            // 获取当前connector配置
            String getUrl = debeziumConnectUrl + "/connectors/" + connectorName + "/config";
            HttpResponse getResponse = HttpRequest.get(getUrl)
                .timeout(10000)
                .execute();

            if (!getResponse.isOk()) {
                log.error("获取连接器配置失败: connector={}, response={}", connectorName, getResponse.body());
                return false;
            }

            // 解析当前配置
            JSONObject currentConfig = JSON.parseObject(getResponse.body());
            Map<String, String> configMap = new HashMap<>();
            for (String key : currentConfig.keySet()) {
                Object value = currentConfig.get(key);
                if (value != null) {
                    configMap.put(key, String.valueOf(value));
                }
            }

            // 更新 table.include.list
            String tableIncludeList = String.join(",", tables);
            configMap.put("table.include.list", tableIncludeList);

            // 重新部署connector
            String putUrl = debeziumConnectUrl + "/connectors/" + connectorName + "/config";
            String jsonBody = JSON.toJSONString(configMap);

            log.info("更新连接器表配置: connector={}, tables={}", connectorName, tableIncludeList);

            HttpResponse putResponse = HttpRequest.put(putUrl)
                .body(jsonBody, "application/json")
                .timeout(30000)
                .execute();

            if (putResponse.isOk()) {
                log.info("更新连接器表配置成功: connector={}, tableCount={}", connectorName, tables.size());
                return true;
            } else {
                log.error("更新连接器表配置失败: connector={}, response={}", connectorName, putResponse.body());
                return false;
            }
        } catch (Exception e) {
            log.error("更新连接器表配置异常: connector={}", connectorName, e);
            return false;
        }
    }

    /**
     * 构建Debezium连接器配置（直接返回config内容，用于PUT /connectors/{name}/config）
     * 注意：Debezium Connect REST API要求所有值必须是字符串
     */
    private Map<String, String> buildConnectorConfigDirect(EtlCdcConfig config, DatabaseConnectionInfo dbInfo) {
        Map<String, String> configMap = new HashMap<>();

        String connectorType = config.getConnectorType();
        String connectorName = config.getConnectorName();

        // 添加连接器名称（Debezium要求config中包含name）
        configMap.put("name", connectorName);

        if ("mysql".equalsIgnoreCase(connectorType)) {
            buildMySQLConnectorConfig(configMap, config, dbInfo);
        } else if ("postgresql".equalsIgnoreCase(connectorType)) {
            buildPostgreSQLConnectorConfig(configMap, config, dbInfo);
        } else {
            throw new RuntimeException("不支持的连接器类型: " + connectorType);
        }

        return configMap;
    }

    /**
     * 构建Debezium连接器配置（旧格式，保留用于其他场景）
     * 注意：Debezium Connect REST API要求config字段内的所有值必须是字符串
     */
    private Map<String, Object> buildConnectorConfig(EtlCdcConfig config, DatabaseConnectionInfo dbInfo) {
        Map<String, Object> connectorConfig = new HashMap<>();

        String connectorType = config.getConnectorType();
        String connectorName = config.getConnectorName();

        connectorConfig.put("name", connectorName);

        Map<String, String> configMap = new HashMap<>();

        if ("mysql".equalsIgnoreCase(connectorType)) {
            buildMySQLConnectorConfig(configMap, config, dbInfo);
        } else if ("postgresql".equalsIgnoreCase(connectorType)) {
            buildPostgreSQLConnectorConfig(configMap, config, dbInfo);
        } else {
            throw new RuntimeException("不支持的连接器类型: " + connectorType);
        }

        connectorConfig.put("config", configMap);
        return connectorConfig;
    }

    /**
     * 构建MySQL连接器配置
     */
    private void buildMySQLConnectorConfig(Map<String, String> configMap, EtlCdcConfig config, DatabaseConnectionInfo dbInfo) {
        configMap.put("connector.class", "io.debezium.connector.mysql.MySqlConnector");

        // 数据库连接配置
        configMap.put("database.hostname", dbInfo.getHost());
        configMap.put("database.port", String.valueOf(dbInfo.getPort()));
        configMap.put("database.user", dbInfo.getUsername());
        configMap.put("database.password", dbInfo.getPassword());
        configMap.put("database.include.list", dbInfo.getDatabase());

        // 服务器名称（用作Topic前缀）
        String serverName = StrUtil.isNotBlank(config.getServerName())
            ? config.getServerName()
            : "etl-mysql-" + config.getId();
        configMap.put("topic.prefix", serverName);

        // 表过滤配置
        if (StrUtil.isNotBlank(config.getFilterRegex())) {
            // Canal格式: db1\\.table1,db2\\.table2
            // Debezium格式: db1.table1,db2.table2
            String tableInclude = convertCanalFilterToDebezium(config.getFilterRegex());
            configMap.put("table.include.list", tableInclude);
        }

        // 黑名单过滤
        if (StrUtil.isNotBlank(config.getFilterBlackRegex())) {
            String tableExclude = convertCanalFilterToDebezium(config.getFilterBlackRegex());
            configMap.put("table.exclude.list", tableExclude);
        }

        // 服务器ID（必须唯一）
        configMap.put("database.server.id", String.valueOf(generateServerId()));

        // 快照配置
        configMap.put("snapshot.mode", "initial");
        configMap.put("snapshot.locking.mode", "minimal");

        // 数据格式配置
        configMap.put("decimal.handling.mode", "string");
        configMap.put("time.precision.mode", "connect");

        // 消息转换配置
        configMap.put("transforms", "unwrap");
        configMap.put("transforms.unwrap.type", "io.debezium.transforms.ExtractNewRecordState");
        configMap.put("transforms.unwrap.drop.tombstones", "false");
        configMap.put("transforms.unwrap.delete.handling.mode", "rewrite");
        configMap.put("transforms.unwrap.add.fields", "op,ts_ms");

        // 转换器配置
        configMap.put("key.converter", "org.apache.kafka.connect.storage.StringConverter");
        configMap.put("value.converter", "org.apache.kafka.connect.json.JsonConverter");
        configMap.put("value.converter.schemas.enable", "false");

        // Schema History配置（用于存储表结构变更）
        configMap.put("schema.history.internal.kafka.bootstrap.servers", kafkaBootstrapServers);
        configMap.put("schema.history.internal.kafka.topic", "schema-changes." + serverName);

        // 解析扩展配置
        if (StrUtil.isNotBlank(config.getExtraConfig())) {
            JSONObject extra = JSON.parseObject(config.getExtraConfig());
            for (String key : extra.keySet()) {
                configMap.put(key, String.valueOf(extra.get(key)));
            }
        }
    }

    /**
     * 构建PostgreSQL连接器配置
     */
    private void buildPostgreSQLConnectorConfig(Map<String, String> configMap, EtlCdcConfig config, DatabaseConnectionInfo dbInfo) {
        configMap.put("connector.class", "io.debezium.connector.postgresql.PostgresConnector");

        // 数据库连接配置
        configMap.put("database.hostname", dbInfo.getHost());
        configMap.put("database.port", String.valueOf(dbInfo.getPort()));
        configMap.put("database.user", dbInfo.getUsername());
        configMap.put("database.password", dbInfo.getPassword());
        configMap.put("database.dbname", dbInfo.getDatabase());

        // 服务器名称（用作Topic前缀）
        String serverName = StrUtil.isNotBlank(config.getServerName())
            ? config.getServerName()
            : "etl-pg-" + config.getId();
        configMap.put("topic.prefix", serverName);

        // 表过滤配置
        if (StrUtil.isNotBlank(config.getFilterRegex())) {
            String tableInclude = convertCanalFilterToDebezium(config.getFilterRegex());
            configMap.put("table.include.list", tableInclude);
        }

        // 黑名单过滤
        if (StrUtil.isNotBlank(config.getFilterBlackRegex())) {
            String tableExclude = convertCanalFilterToDebezium(config.getFilterBlackRegex());
            configMap.put("table.exclude.list", tableExclude);
        }

        // PostgreSQL特定配置
        configMap.put("plugin.name", "pgoutput");
        configMap.put("publication.name", "etl_publication_" + config.getId());
        configMap.put("slot.name", "etl_slot_" + config.getId());

        // 快照配置
        configMap.put("snapshot.mode", "initial");

        // 数据格式配置
        configMap.put("decimal.handling.mode", "string");
        configMap.put("time.precision.mode", "connect");

        // 消息转换配置
        configMap.put("transforms", "unwrap");
        configMap.put("transforms.unwrap.type", "io.debezium.transforms.ExtractNewRecordState");
        configMap.put("transforms.unwrap.drop.tombstones", "false");
        configMap.put("transforms.unwrap.delete.handling.mode", "rewrite");
        configMap.put("transforms.unwrap.add.fields", "op,ts_ms");

        // 转换器配置
        configMap.put("key.converter", "org.apache.kafka.connect.storage.StringConverter");
        configMap.put("value.converter", "org.apache.kafka.connect.json.JsonConverter");
        configMap.put("value.converter.schemas.enable", "false");

        // 解析扩展配置
        if (StrUtil.isNotBlank(config.getExtraConfig())) {
            JSONObject extra = JSON.parseObject(config.getExtraConfig());
            for (String key : extra.keySet()) {
                configMap.put(key, String.valueOf(extra.get(key)));
            }
        }
    }

    /**
     * 获取数据库连接信息
     */
    private DatabaseConnectionInfo getDatabaseConnectionInfo(EtlCdcConfig config) {
        EtlDatasource datasource = datasourceService.getById(config.getDatasourceId());
        if (datasource == null) {
            throw new RuntimeException("数据源不存在: " + config.getDatasourceId());
        }

        DatabaseConnectionInfo info = new DatabaseConnectionInfo();

        // 优先使用CDC配置中的覆盖值
        info.setHost(StrUtil.isNotBlank(config.getDatabaseHost())
            ? config.getDatabaseHost()
            : datasource.getHost());
        info.setPort(config.getDatabasePort() != null
            ? config.getDatabasePort()
            : datasource.getPort());
        info.setUsername(StrUtil.isNotBlank(config.getDbUsername())
            ? config.getDbUsername()
            : datasource.getUsername());
        info.setPassword(StrUtil.isNotBlank(config.getDbPassword())
            ? EncryptionUtil.decrypt(config.getDbPassword())
            : EncryptionUtil.decrypt(datasource.getPassword()));
        info.setDatabase(datasource.getDatabaseName());

        return info;
    }

    /**
     * 转换Canal过滤格式到Debezium格式
     * Canal: db1\\.table1,db2\\.table2
     * Debezium: db1.table1,db2.table2
     */
    private String convertCanalFilterToDebezium(String canalFilter) {
        if (StrUtil.isBlank(canalFilter)) {
            return null;
        }
        // 移除转义的反斜杠
        return canalFilter.replace("\\.", ".");
    }

    private static final java.util.concurrent.atomic.AtomicLong SERVER_ID_SEQ = new java.util.concurrent.atomic.AtomicLong(0);

    /**
     * 生成唯一的服务器ID
     */
    private long generateServerId() {
        return 5400 + SERVER_ID_SEQ.incrementAndGet();
    }

    /**
     * 数据库连接信息
     */
    @lombok.Data
    public static class DatabaseConnectionInfo {
        private String host;
        private Integer port;
        private String username;
        private String password;
        private String database;
    }
}
