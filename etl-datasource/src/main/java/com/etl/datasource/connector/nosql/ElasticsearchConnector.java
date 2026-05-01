package com.etl.datasource.connector.nosql;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.HealthStatus;
import co.elastic.clients.elasticsearch.cat.IndicesResponse;
import co.elastic.clients.elasticsearch.indices.GetMappingResponse;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.etl.common.domain.ColumnInfo;
import com.etl.common.domain.TableInfo;
import com.etl.common.enums.DataSourceType;
import com.etl.datasource.connector.DatabaseConnector;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Elasticsearch 数据源连接器
 */
@Slf4j
public class ElasticsearchConnector implements DatabaseConnector {

    private final Long datasourceId;
    private final String host;
    private final Integer port;
    private final String username;
    private final String password;

    private RestClient restClient;
    private ElasticsearchClient esClient;

    public ElasticsearchConnector(Long datasourceId, String host, Integer port, String databaseName,
                                  String username, String password, String charset, String extraConfig) {
        this.datasourceId = datasourceId;
        this.host = host;
        this.port = port != null ? port : 9200;
        this.username = username;
        this.password = password;
    }

    private void initClient() {
        if (esClient == null) {
            RestClientBuilder builder = RestClient.builder(new HttpHost(host, port, "http"));

            if (username != null && !username.isEmpty()) {
                BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(
                    AuthScope.ANY,
                    new UsernamePasswordCredentials(username, password)
                );
                builder.setHttpClientConfigCallback(httpClientBuilder ->
                    httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider)
                );
            }

            restClient = builder.build();
            esClient = new ElasticsearchClient(new RestClientTransport(restClient, new JacksonJsonpMapper()));
            log.info("Elasticsearch 连接成功: {}:{}", host, port);
        }
    }

    public ElasticsearchClient getEsClient() {
        initClient();
        return esClient;
    }

    @Override
    public java.sql.Connection getConnection() throws Exception {
        throw new UnsupportedOperationException("Elasticsearch 使用 REST API，不支持 JDBC Connection");
    }

    @Override
    public boolean testConnection() {
        try {
            initClient();
            var health = esClient.cluster().health();
            return health.status() != HealthStatus.Red;
        } catch (Exception e) {
            log.error("Elasticsearch 连接测试失败", e);
            return false;
        }
    }

    @Override
    public List<String> listDatabases() throws Exception {
        // ES 没有 database 概念，返回索引别名
        List<String> indices = new ArrayList<>();
        initClient();
        IndicesResponse response = esClient.cat().indices();
        response.valueBody().forEach(record -> indices.add(record.index()));
        return indices;
    }

    @Override
    public List<TableInfo> getTables() throws Exception {
        return getTables(null);
    }

    @Override
    public List<TableInfo> getTables(String database) throws Exception {
        List<TableInfo> tables = new ArrayList<>();
        initClient();
        IndicesResponse response = esClient.cat().indices();
        response.valueBody().forEach(record -> {
            TableInfo tableInfo = new TableInfo();
            tableInfo.setTableName(record.index());
            tableInfo.setTableType("INDEX");
            tableInfo.setSchemaName(null);
            tables.add(tableInfo);
        });
        return tables;
    }

    @Override
    public TableInfo getTableInfo(String tableName) throws Exception {
        initClient();
        TableInfo tableInfo = new TableInfo();
        tableInfo.setTableName(tableName);
        tableInfo.setTableType("INDEX");
        tableInfo.setColumns(getColumns(tableName));
        return tableInfo;
    }

    @Override
    public List<ColumnInfo> getColumns(String tableName) throws Exception {
        List<ColumnInfo> columns = new ArrayList<>();
        initClient();

        GetMappingResponse response = esClient.indices().getMapping(m -> m.index(tableName));
        var mappings = response.get(tableName);
        if (mappings == null) {
            return columns;
        }
        var mappingData = mappings.mappings();
        if (mappingData == null || mappingData.properties() == null) {
            return columns;
        }
        int ordinal = 1;
        for (Map.Entry<String, ?> entry : mappingData.properties().entrySet()) {
            ColumnInfo column = new ColumnInfo();
            column.setColumnName(entry.getKey());
            column.setColumnType(getFieldTypeName(entry.getValue()));
            column.setNullable(true);
            column.setOrdinalPosition(ordinal++);
            columns.add(column);
        }
        return columns;
    }

    private String getFieldTypeName(Object field) {
        // 简化实现，实际需要解析 Property 的具体类型
        if (field instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) field;
            if (map.containsKey("type")) {
                return map.get("type").toString();
            }
        }
        return "object";
    }

    @Override
    public List<String> getPrimaryKeys(String tableName) throws Exception {
        // ES 默认主键是 _id
        return List.of("_id");
    }

    @Override
    public long getRowCount(String tableName) throws Exception {
        initClient();
        var response = esClient.count(c -> c.index(tableName));
        return response.count();
    }

    @Override
    public void close() {
        if (restClient != null) {
            try {
                restClient.close();
            } catch (Exception e) {
                log.error("关闭 Elasticsearch 连接失败", e);
            }
            restClient = null;
            esClient = null;
        }
    }

    @Override
    public String getDatabaseType() {
        return "ELASTICSEARCH";
    }

    @Override
    public String getJdbcUrl() {
        return String.format("http://%s:%d", host, port);
    }

    @Override
    public String getHost() { return host; }

    @Override
    public Integer getPort() { return port; }

    @Override
    public String getDatabaseName() { return null; }

    @Override
    public String getUsername() { return username; }

    @Override
    public String getPassword() { return password; }

    @Override
    public DataSourceType supportsType() {
        return DataSourceType.ELASTICSEARCH;
    }
}
