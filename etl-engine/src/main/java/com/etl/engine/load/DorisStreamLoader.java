package com.etl.engine.load;

import com.alibaba.fastjson2.JSON;
import com.etl.common.utils.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Doris Stream Load 数据加载器
 * 使用 HTTP PUT 方式发送数据到 Doris
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "engine.feature.stream-load-doris.enabled", havingValue = "true", matchIfMissing = true)
public class DorisStreamLoader {

    /**
     * 使用 Stream Load 加载数据到 Doris
     *
     * @param feHost FE 主机地址
     * @param feHttpPort FE HTTP 端口
     * @param database 数据库名
     * @param table 表名
     * @param data 数据列表
     * @return 成功处理行数
     */
    public long streamLoad(String feHost, int feHttpPort, String database, String table,
                           List<Map<String, Object>> data) throws Exception {

        if (data == null || data.isEmpty()) {
            log.debug("没有数据需要加载到 Doris 表 {}.{} 中", database, table);
            return 0;
        }

        // 1. 构建请求 URL
        String urlStr = String.format("http://%s:%d/api/%s/%s/_stream_load",
            feHost, feHttpPort, database, table);

        // 2. 转换数据为 CSV 格式
        String csvData = convertToCsv(data);
        log.debug("转换后 CSV 数据大小: {} bytes", csvData.getBytes(StandardCharsets.UTF_8).length);

        // 3. 发送 HTTP 请求
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("PUT");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(30000);

            // 设置请求属性
            String label = generateUniqueLabel(database, table);
            conn.setRequestProperty("Content-Type", "text/plain; charset=utf-8");
            conn.setRequestProperty("label", label);
            conn.setRequestProperty("column_separator", ",");
            conn.setRequestProperty("line_delimiter", "\\n");
            conn.setRequestProperty("max_filter_ratio", "0.01"); // 允许 1% 的数据过滤
            conn.setDoOutput(true);

            // 发送数据
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = csvData.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // 获取响应
            int responseCode = conn.getResponseCode();
            String responseBody = readResponse(conn);

            log.debug("Stream Load 响应: 状态码={}, 响应内容={}", responseCode, responseBody);

            // 解析响应
            Map<String, Object> result = JSON.parseObject(responseBody);
            if (responseCode == HttpURLConnection.HTTP_OK) {
                String status = (String) result.get("Status");
                if ("Success".equals(status)) {
                    Number loadedRows = (Number) result.get("NumberLoadedRows");
                    log.debug("Stream Load 成功: 加载行数={}", loadedRows);
                    return loadedRows != null ? loadedRows.longValue() : 0;
                } else {
                    String message = (String) result.get("Message");
                    throw new RuntimeException(String.format("Stream Load 失败: %s", message));
                }
            } else {
                throw new RuntimeException(String.format("Stream Load 请求失败: 状态码=%d, 响应=%s",
                    responseCode, responseBody));
            }

        } catch (Exception e) {
            log.error("Stream Load 失败: {}.{}", database, table, e);
            throw e;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * 转换数据为 CSV 格式（使用 LinkedHashMap 保证列顺序一致）
     */
    private String convertToCsv(List<Map<String, Object>> data) {
        if (data.isEmpty()) {
            return "";
        }

        // 使用第一行的keySet确定列顺序，统一用List保持顺序
        Map<String, Object> firstRow = data.get(0);
        List<String> headers = new ArrayList<>();
        if (firstRow instanceof LinkedHashMap) {
            headers.addAll(firstRow.keySet());
        } else {
            // 对 key 排序以保证一致的列顺序
            headers = new ArrayList<>(firstRow.keySet());
            java.util.Collections.sort(headers);
        }

        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> row : data) {
            List<String> values = new ArrayList<>(headers.size());
            for (String header : headers) {
                Object value = row.get(header);
                values.add(formatCsvValue(value));
            }
            sb.append(String.join(",", values)).append("\n");
        }

        return sb.toString();
    }

    /**
     * 格式化 CSV 值
     */
    private String formatCsvValue(Object value) {
        if (value == null) {
            return "\\N";
        }
        String strValue = value.toString();
        // 处理包含引号和特殊字符的情况
        if (strValue.contains("\"")) {
            strValue = strValue.replace("\"", "\"\"");
        }
        if (strValue.contains(",") || strValue.contains("\"") || strValue.contains("\n")) {
            return "\"" + strValue + "\"";
        }
        return strValue;
    }

    /**
     * 生成唯一的 label
     */
    private String generateUniqueLabel(String database, String table) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return String.format("etl_sync_%s_%s_%s_%s",
            database, table, timestamp, random);
    }

    /**
     * 读取响应
     */
    private String readResponse(HttpURLConnection conn) throws Exception {
        InputStream is = null;
        try {
            if (conn.getResponseCode() < HttpURLConnection.HTTP_BAD_REQUEST) {
                is = conn.getInputStream();
            } else {
                is = conn.getErrorStream();
            }
            StringBuilder sb = new StringBuilder();
            try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                char[] buffer = new char[1024];
                int length;
                while ((length = reader.read(buffer)) != -1) {
                    sb.append(buffer, 0, length);
                }
            }
            return sb.toString();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception e) {
                    log.error("关闭输入流失败", e);
                }
            }
        }
    }
}
