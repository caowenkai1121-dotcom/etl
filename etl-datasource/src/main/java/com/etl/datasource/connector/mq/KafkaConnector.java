package com.etl.datasource.connector.mq;

import com.etl.common.domain.ColumnInfo;
import com.etl.common.domain.TableInfo;
import com.etl.common.enums.DataSourceType;
import com.etl.datasource.connector.CdcConfig;
import com.etl.datasource.connector.CdcReader;
import com.etl.datasource.connector.DatabaseConnector;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.admin.TopicListing;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * Kafka 消息队列连接器
 * 可作为数据源消费消息，或作为目标发送消息
 */
@Slf4j
public class KafkaConnector implements DatabaseConnector {

    private final Long datasourceId;
    private final String host;
    private final Integer port;
    private final String databaseName; // 作为 consumer group
    private final String username;
    private final String password;

    private AdminClient adminClient;
    private Properties consumerProps;

    public KafkaConnector(Long datasourceId, String host, Integer port, String databaseName,
                          String username, String password, String charset, String extraConfig) {
        this.datasourceId = datasourceId;
        this.host = host;
        this.port = port != null ? port : 9092;
        this.databaseName = databaseName != null ? databaseName : "etl-sync-group";
        this.username = username;
        this.password = password;
        initProperties();
    }

    private void initProperties() {
        consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, host + ":" + port);
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, databaseName);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        // SASL 认证
        if (username != null && !username.isEmpty()) {
            consumerProps.put("security.protocol", "SASL_PLAINTEXT");
            consumerProps.put("sasl.mechanism", "PLAIN");
            consumerProps.put("sasl.jaas.config",
                String.format("org.apache.kafka.common.security.plain.PlainLoginModule required username=\"%s\" password=\"%s\";",
                    username, password));
        }
    }

    private AdminClient getAdminClient() {
        if (adminClient == null) {
            Properties props = new Properties();
            props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, host + ":" + port);
            adminClient = AdminClient.create(props);
            log.info("Kafka AdminClient 连接成功: {}:{}", host, port);
        }
        return adminClient;
    }

    @Override
    public java.sql.Connection getConnection() throws Exception {
        throw new UnsupportedOperationException("Kafka 使用 KafkaConsumer，不支持 JDBC Connection");
    }

    @Override
    public boolean testConnection() {
        try {
            ListTopicsResult result = getAdminClient().listTopics();
            result.names().get();
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Kafka 连接测试被中断", e);
            return false;
        } catch (ExecutionException e) {
            log.error("Kafka 连接测试失败", e);
            return false;
        }
    }

    @Override
    public List<String> listDatabases() throws Exception {
        // Kafka 没有 database 概念，返回空列表
        return new ArrayList<>();
    }

    @Override
    public List<TableInfo> getTables() throws Exception {
        List<TableInfo> tables = new ArrayList<>();
        ListTopicsResult result = getAdminClient().listTopics();

        for (TopicListing topic : result.listings().get()) {
            TableInfo tableInfo = new TableInfo();
            tableInfo.setTableName(topic.name());
            tableInfo.setTableType("TOPIC");
            tables.add(tableInfo);
        }

        return tables;
    }

    @Override
    public List<TableInfo> getTables(String database) throws Exception {
        return getTables();
    }

    @Override
    public TableInfo getTableInfo(String tableName) throws Exception {
        TableInfo tableInfo = new TableInfo();
        tableInfo.setTableName(tableName);
        tableInfo.setTableType("TOPIC");
        tableInfo.setColumns(getColumns(tableName));
        return tableInfo;
    }

    @Override
    public List<ColumnInfo> getColumns(String tableName) throws Exception {
        // Kafka 消息的默认字段
        List<ColumnInfo> columns = new ArrayList<>();

        ColumnInfo keyColumn = new ColumnInfo();
        keyColumn.setColumnName("key");
        keyColumn.setColumnType("string");
        keyColumn.setOrdinalPosition(1);
        columns.add(keyColumn);

        ColumnInfo valueColumn = new ColumnInfo();
        valueColumn.setColumnName("value");
        valueColumn.setColumnType("string");
        valueColumn.setOrdinalPosition(2);
        columns.add(valueColumn);

        ColumnInfo partitionColumn = new ColumnInfo();
        partitionColumn.setColumnName("partition");
        partitionColumn.setColumnType("int");
        partitionColumn.setOrdinalPosition(3);
        columns.add(partitionColumn);

        ColumnInfo offsetColumn = new ColumnInfo();
        offsetColumn.setColumnName("offset");
        offsetColumn.setColumnType("bigint");
        offsetColumn.setOrdinalPosition(4);
        columns.add(offsetColumn);

        ColumnInfo timestampColumn = new ColumnInfo();
        timestampColumn.setColumnName("timestamp");
        timestampColumn.setColumnType("bigint");
        timestampColumn.setOrdinalPosition(5);
        columns.add(timestampColumn);

        return columns;
    }

    @Override
    public List<String> getPrimaryKeys(String tableName) throws Exception {
        return List.of("key", "partition", "offset");
    }

    @Override
    public long getRowCount(String tableName) throws Exception {
        // 获取 Topic 的消息数（近似值）
        // 需要通过 Consumer 实现
        return -1;
    }

    @Override
    public void close() {
        if (adminClient != null) {
            adminClient.close();
            adminClient = null;
        }
    }

    /**
     * 创建 Kafka Consumer
     */
    public KafkaConsumer<String, String> createConsumer() {
        return new KafkaConsumer<>(consumerProps);
    }

    /**
     * 创建 Kafka Consumer 并订阅 Topic
     */
    public KafkaConsumer<String, String> createConsumer(String... topics) {
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps);
        consumer.subscribe(Arrays.asList(topics));
        return consumer;
    }

    @Override
    public String getDatabaseType() {
        return "KAFKA";
    }

    @Override
    public String getJdbcUrl() {
        return String.format("kafka://%s:%d", host, port);
    }

    @Override
    public String getHost() { return host; }

    @Override
    public Integer getPort() { return port; }

    @Override
    public String getDatabaseName() { return databaseName; }

    @Override
    public String getUsername() { return username; }

    @Override
    public String getPassword() { return password; }

    @Override
    public boolean supportsCdc() {
        return true;
    }

    @Override
    public DataSourceType supportsType() {
        return DataSourceType.KAFKA;
    }
}
