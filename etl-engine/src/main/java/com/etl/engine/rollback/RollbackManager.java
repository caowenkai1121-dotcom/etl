package com.etl.engine.rollback;

import com.etl.datasource.connector.DatabaseConnector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.Statement;

/**
 * 回滚管理器
 * 提供临时表方案实现失败回滚功能
 */
@Slf4j
@Component
public class RollbackManager {

    /**
     * 开始回滚会话
     *
     * @param targetConnector 目标数据库连接器
     * @param targetTable     目标表名
     * @return 回滚会话
     * @throws Exception 异常
     */
    public RollbackSession beginRollbackSession(DatabaseConnector targetConnector,
                                                  String targetTable) throws Exception {
        return new RollbackSession(targetConnector, targetTable);
    }

    /**
     * 回滚会话
     * 使用临时表方案实现原子替换
     */
    public static class RollbackSession implements AutoCloseable {
        private final DatabaseConnector connector;
        private final String targetTable;
        private final String tmpTable;
        private final String bakTable;
        private final String dbType;
        private boolean committed = false;
        private boolean rolledBack = false;

        public RollbackSession(DatabaseConnector connector, String targetTable) {
            this.connector = connector;
            this.targetTable = targetTable;
            this.tmpTable = targetTable + "_tmp";
            this.bakTable = targetTable + "_bak";
            this.dbType = connector.getDatabaseType();
        }

        /**
         * 获取临时表名
         *
         * @return 临时表名
         */
        public String getTmpTable() {
            return tmpTable;
        }

        /**
         * 准备临时表
         *
         * @throws Exception 异常
         */
        public void prepare() throws Exception {
            try (Connection conn = connector.getConnection();
                 Statement stmt = conn.createStatement()) {
                if ("POSTGRESQL".equalsIgnoreCase(dbType)) {
                    stmt.execute("CREATE TABLE " + quote(tmpTable) + " (LIKE " + quote(targetTable) + " INCLUDING ALL)");
                } else {
                    stmt.execute("CREATE TABLE " + quote(tmpTable) + " LIKE " + quote(targetTable));
                }
            }
            log.info("创建临时表: {}", tmpTable);
        }

        /**
         * 提交会话：用临时表替换目标表
         *
         * @throws Exception 异常
         */
        public void commit() throws Exception {
            try (Connection conn = connector.getConnection();
                 Statement stmt = conn.createStatement()) {
                if ("POSTGRESQL".equalsIgnoreCase(dbType)) {
                    stmt.execute("DROP TABLE IF EXISTS " + quote(bakTable));
                    stmt.execute("ALTER TABLE " + quote(targetTable) + " RENAME TO " + quote(bakTable));
                    stmt.execute("ALTER TABLE " + quote(tmpTable) + " RENAME TO " + quote(targetTable));
                    stmt.execute("DROP TABLE IF EXISTS " + quote(bakTable));
                } else {
                    stmt.execute("DROP TABLE IF EXISTS " + quote(bakTable));
                    stmt.execute("RENAME TABLE " + quote(targetTable) + " TO " + quote(bakTable)
                            + ", " + quote(tmpTable) + " TO " + quote(targetTable));
                    stmt.execute("DROP TABLE IF EXISTS " + quote(bakTable));
                }
            }
            committed = true;
            log.info("回滚会话提交: 临时表 {} 替换目标表 {}", tmpTable, targetTable);
        }

        /**
         * 回滚会话：删除临时表
         *
         * @throws Exception 异常
         */
        public void rollback() throws Exception {
            if (committed) return;
            try (Connection conn = connector.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("DROP TABLE IF EXISTS " + quote(tmpTable));
            }
            rolledBack = true;
            log.info("回滚会话回滚: 删除临时表 {}", tmpTable);
        }

        /**
         * 关闭资源时自动回滚（如果未提交）
         */
        @Override
        public void close() {
            if (!committed && !rolledBack) {
                try {
                    rollback();
                } catch (Exception e) {
                    log.error("关闭回滚会话失败", e);
                }
            }
        }

        /**
         * 引用标识符
         *
         * @param identifier 标识符
         * @return 引用后的标识符
         */
        private String quote(String identifier) {
            if ("POSTGRESQL".equalsIgnoreCase(dbType)) {
                return "\"" + identifier + "\"";
            }
            return "`" + identifier + "`";
        }
    }
}
