package com.etl.engine.cdc;

import com.alibaba.fastjson2.JSONObject;
import com.etl.common.enums.CdcEventType;
import com.etl.common.domain.CdcEvent;
import io.debezium.config.Configuration;
import io.debezium.embedded.Connect;
import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.format.Json;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.connect.source.SourceRecord;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Debezium客户端
 * 用于监听PostgreSQL WAL变更
 *
 * 使用嵌入模式运行Debezium引擎
 */
@Slf4j
public class DebeziumClient {

    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private final String serverName;
    private final Set<String> tableSet;

    private DebeziumEngine<ChangeEvent<SourceRecord, SourceRecord>> engine;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final BlockingQueue<CdcEvent> eventQueue = new LinkedBlockingQueue<>(10000);

    public DebeziumClient(String host, int port, String database, String username,
                          String password, String serverName, Set<String> tables) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
        this.serverName = serverName;
        this.tableSet = tables;
    }

    /**
     * 启动Debezium引擎
     */
    public void start() {
        Configuration config = Configuration.create()
            .with("name", serverName)
            .with("connector.class", "io.debezium.connector.postgresql.PostgresConnector")
            .with("offset.storage", "org.apache.kafka.connect.storage.FileOffsetBackingStore")
            .with("offset.storage.file.filename", "/tmp/debezium-offsets-" + serverName + ".dat")
            .with("offset.flush.interval.ms", 60000)
            .with("database.hostname", host)
            .with("database.port", port)
            .with("database.user", username)
            .with("database.password", password)
            .with("database.dbname", database)
            .with("database.server.name", serverName)
            .with("plugin.name", "pgoutput")
            .with("slot.name", "debezium_slot_" + serverName)
            .build();

        // 使用Consumer处理事件
        Consumer<ChangeEvent<SourceRecord, SourceRecord>> consumer = event -> {
            try {
                handleEvent(event);
            } catch (Exception e) {
                log.error("处理Debezium事件失败", e);
            }
        };

        // 使用正确的Builder模式
        DebeziumEngine.Builder<ChangeEvent<SourceRecord, SourceRecord>> builder = DebeziumEngine.create(Connect.class);
        builder.using(config.asProperties())
               .notifying(consumer)
               .using((success, message, error) -> {
                   if (!success && error != null) {
                       log.error("Debezium引擎异常: {}", message, error);
                   }
               });

        engine = builder.build();

        running.set(true);

        new Thread(() -> {
            try {
                engine.run();
            } catch (Exception e) {
                log.error("Debezium引擎运行异常", e);
            }
        }, "debezium-engine").start();

        log.info("Debezium客户端启动成功: {}:{}", host, port);
    }

    /**
     * 处理Debezium事件
     */
    @SuppressWarnings("unchecked")
    private void handleEvent(ChangeEvent<SourceRecord, SourceRecord> event) {
        try {
            SourceRecord record = event.value();
            if (record == null) {
                return;
            }

            // 获取表名和数据库名
            String topic = record.topic();
            String[] parts = topic.split("\\.");
            String databaseName = parts.length > 0 ? parts[0] : null;
            String tableName = parts.length > 2 ? parts[2] : null;

            // 过滤表
            if (tableSet != null && !tableSet.isEmpty() && (tableName == null || !tableSet.contains(tableName))) {
                return;
            }

            // 获取操作类型
            Map<String, ?> sourceOffset = record.sourceOffset();
            Map<String, ?> partition = record.sourcePartition();

            // 解析变更数据
            Object value = record.value();
            if (!(value instanceof Map)) {
                return;
            }

            Map<String, Object> valueMap = (Map<String, Object>) value;
            Map<String, Object> payload = (Map<String, Object>) valueMap.get("payload");

            if (payload == null) {
                payload = valueMap;
            }

            // 确定操作类型
            String op = (String) payload.get("op");
            if (op == null) {
                return;
            }
            CdcEventType eventType = switch (op) {
                case "c" -> CdcEventType.INSERT;
                case "u" -> CdcEventType.UPDATE;
                case "d" -> CdcEventType.DELETE;
                default -> null;
            };

            if (eventType == null) {
                return;
            }

            CdcEvent cdcEvent = new CdcEvent();
            cdcEvent.setDatabase(databaseName);
            cdcEvent.setTable(tableName);
            cdcEvent.setEventType(eventType);
            cdcEvent.setTimestamp(System.currentTimeMillis());

            // 解析数据
            Map<String, Object> after = (Map<String, Object>) payload.get("after");
            Map<String, Object> before = (Map<String, Object>) payload.get("before");

            if (after != null) {
                cdcEvent.setAfterData(new HashMap<>(after));
            }
            if (before != null) {
                cdcEvent.setBeforeData(new HashMap<>(before));
            }

            eventQueue.offer(cdcEvent);

        } catch (Exception e) {
            log.error("处理Debezium事件失败", e);
        }
    }

    /**
     * 获取CDC事件
     */
    public CdcEvent pollEvent() throws InterruptedException {
        return eventQueue.take();
    }

    /**
     * 获取CDC事件（带超时）
     */
    public CdcEvent pollEvent(long timeoutMs) throws InterruptedException {
        return eventQueue.poll(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    /**
     * 停止Debezium引擎
     */
    public void stop() {
        running.set(false);
        if (engine != null) {
            try {
                engine.close();
            } catch (IOException e) {
                log.error("关闭Debezium引擎失败", e);
            }
        }
        log.info("Debezium客户端已停止");
    }

    public boolean isRunning() {
        return running.get();
    }

    /**
     * 转换为Kafka消息格式
     */
    public static String toKafkaMessage(CdcEvent event) {
        JSONObject json = new JSONObject();
        json.put("database", event.getDatabase());
        json.put("table", event.getTable());
        json.put("type", event.getEventType().getCode());
        json.put("ts", event.getTimestamp());
        json.put("position", event.getPosition());

        if (event.getAfterData() != null) {
            json.put("data", event.getAfterData());
        }
        if (event.getBeforeData() != null) {
            json.put("old", event.getBeforeData());
        }

        return json.toJSONString();
    }
}