package com.etl.engine.load;

import com.etl.datasource.connector.DatabaseConnector;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 批量数据加载器
 */
@Slf4j
@Component
public class BatchLoader {

    /**
     * 批量加载数据
     *
     * @param targetConnector 目标数据库连接器
     * @param targetTable 目标表名
     * @param data 数据列表
     * @param config 加载配置
     * @return 成功写入行数
     */
    public long load(DatabaseConnector targetConnector, String targetTable,
                     List<Map<String, Object>> data, LoadConfig config) throws Exception {

        if (data == null || data.isEmpty()) {
            log.debug("没有数据需要加载到表 {} 中", targetTable);
            return 0;
        }

        List<String> columnNames = new ArrayList<>();
        if (!data.isEmpty()) {
            columnNames.addAll(data.get(0).keySet());
        }
        String[] columns = columnNames.toArray(new String[0]);

        // 构建SQL
        String sql = buildInsertSql(targetConnector, targetTable, columns, data.size());
        log.debug("使用SQL: {}", sql);

        long successRows = 0;
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = targetConnector.getConnection();
            conn.setAutoCommit(false);
            stmt = conn.prepareStatement(sql);

            // 设置参数并添加到批次
            for (Map<String, Object> row : data) {
                int parameterIndex = 1;
                for (String column : columns) {
                    stmt.setObject(parameterIndex++, row.get(column));
                }
                stmt.addBatch();
            }

            // 执行批量插入
            int[] result = stmt.executeBatch();

            // 计算成功行数
            for (int count : result) {
                if (count > 0) {
                    successRows += count;
                } else if (count == Statement.SUCCESS_NO_INFO) {
                    successRows++;
                }
            }

            // 提交事务
            conn.commit();
            log.debug("成功加载 {} 条数据到表 {}", successRows, targetTable);
        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (Exception rollbackEx) {
                    log.error("回滚失败", rollbackEx);
                }
            }
            log.error("加载数据到表 {} 失败", targetTable, e);
            throw e;
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (Exception e) {
                    log.error("关闭Statement失败", e);
                }
            }
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (Exception e) {
                    log.error("关闭Connection失败", e);
                }
            }
        }

        return successRows;
    }

    /**
     * 使用默认配置加载数据
     */
    public long load(DatabaseConnector targetConnector, String targetTable, List<Map<String, Object>> data) throws Exception {
        return load(targetConnector, targetTable, data, LoadConfig.defaultConfig());
    }

    /**
     * 构建插入SQL
     */
    private String buildInsertSql(DatabaseConnector connector, String tableName, String[] columns, int rowCount) {
        String quotedTable = quoteIdentifier(connector, tableName);
        StringBuilder sb = new StringBuilder("INSERT INTO ").append(quotedTable).append(" (");

        for (int i = 0; i < columns.length; i++) {
            sb.append(quoteIdentifier(connector, columns[i]));
            if (i < columns.length - 1) {
                sb.append(", ");
            }
        }
        sb.append(") VALUES ");

        for (int i = 0; i < rowCount; i++) {
            sb.append("(");
            for (int j = 0; j < columns.length; j++) {
                sb.append("?");
                if (j < columns.length - 1) {
                    sb.append(", ");
                }
            }
            sb.append(")");
            if (i < rowCount - 1) {
                sb.append(", ");
            }
        }

        return sb.toString();
    }

    /**
     * 引用标识符
     */
    private String quoteIdentifier(DatabaseConnector connector, String identifier) {
        String dbType = connector.getDatabaseType();
        if ("postgresql".equalsIgnoreCase(dbType) || "oracle".equalsIgnoreCase(dbType)) {
            return "\"" + identifier.replace("\"", "\"\"") + "\"";
        } else if ("sqlserver".equalsIgnoreCase(dbType)) {
            return "[" + identifier + "]";
        }
        return "`" + identifier.replace("`", "``") + "`";
    }

    /**
     * 加载配置
     */
    @Data
    public static class LoadConfig {
        /**
         * 批量大小
         */
        private int batchSize = 1000;

        /**
         * 提交批次大小
         */
        private int commitBatchSize = 5000;

        /**
         * 是否使用UPSERT（插入或更新）
         */
        private boolean upsert = false;

        /**
         * 主键字段数组（用于UPSERT）
         */
        private String[] primaryKeys;

        /**
         * 创建默认配置
         */
        public static LoadConfig defaultConfig() {
            return new LoadConfig();
        }

        /**
         * 创建UPSERT配置
         */
        public static LoadConfig upsertConfig(String... primaryKeys) {
            LoadConfig config = new LoadConfig();
            config.setUpsert(true);
            config.setPrimaryKeys(primaryKeys);
            return config;
        }
    }
}
