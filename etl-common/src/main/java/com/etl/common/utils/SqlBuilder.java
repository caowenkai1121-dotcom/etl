package com.etl.common.utils;

/**
 * SQL构建工具类
 */
public class SqlBuilder {

    // 数据库类型常量
    public static final String DB_TYPE_MYSQL = "mysql";
    public static final String DB_TYPE_POSTGRESQL = "postgresql";
    public static final String DB_TYPE_DORIS = "doris";

    private SqlBuilder() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    /**
     * 引用标识符（默认MySQL风格反引号）
     */
    public static String quoteIdentifier(String identifier) {
        return quoteIdentifier(identifier, DB_TYPE_MYSQL);
    }

    /**
     * 引用标识符
     * - PostgreSQL使用双引号
     * - 其他数据库使用反引号
     */
    public static String quoteIdentifier(String identifier, String dbType) {
        if (identifier == null || identifier.isEmpty()) {
            return "";
        }
        if (DB_TYPE_POSTGRESQL.equalsIgnoreCase(dbType)) {
            return "\"" + identifier.replace("\"", "\"\"") + "\"";
        }
        return "`" + identifier.replace("`", "``") + "`";
    }

    /**
     * 构建批量提取SQL（游标分页）
     */
    public static String buildBatchExtractSql(String table, String primaryKey, long lastId, int batchSize, String dbType) {
        String quotedTable = quoteIdentifier(table, dbType);
        String quotedPk = quoteIdentifier(primaryKey, dbType);
        return "SELECT * FROM " + quotedTable +
               " WHERE " + quotedPk + " > " + lastId +
               " ORDER BY " + quotedPk +
               " LIMIT " + batchSize;
    }

    /**
     * 构建增量查询SQL（使用>=）
     */
    public static String buildIncrementalQuerySql(String table, String fieldName, String dbType) {
        String quotedTable = quoteIdentifier(table, dbType);
        String quotedField = quoteIdentifier(fieldName, dbType);
        return "SELECT * FROM " + quotedTable +
               " WHERE " + quotedField + " >= ?";
    }

    /**
     * 构建COUNT查询SQL
     */
    public static String buildCountSql(String table, String dbType) {
        String quotedTable = quoteIdentifier(table, dbType);
        return "SELECT COUNT(*) FROM " + quotedTable;
    }

    /**
     * 构建近似行数查询SQL
     * - PostgreSQL使用pg_class
     * - MySQL使用information_schema
     * 注意：table参数建议先经过验证或使用参数化查询
     */
    public static String buildApproximateCountSql(String table, String dbType) {
        // 对表名进行基本验证，防止SQL注入
        if (table == null || table.trim().isEmpty()) {
            throw new IllegalArgumentException("表名不能为空");
        }
        String safeTable = table.replaceAll("[;'\"\\-\\-]", "");
        if (DB_TYPE_POSTGRESQL.equalsIgnoreCase(dbType)) {
            return "SELECT reltuples AS approximate_count " +
                   "FROM pg_class " +
                   "WHERE relname = '" + safeTable + "'";
        }
        return "SELECT TABLE_ROWS AS approximate_count " +
               "FROM information_schema.TABLES " +
               "WHERE TABLE_NAME = '" + safeTable + "'";
    }

    /**
     * 构建多值INSERT SQL
     */
    public static String buildMultiValueInsertSql(String table, String[] columns, int rowCount, String dbType) {
        String quotedTable = quoteIdentifier(table, dbType);
        StringBuilder sb = new StringBuilder("INSERT INTO " + quotedTable + " (");

        for (int i = 0; i < columns.length; i++) {
            sb.append(quoteIdentifier(columns[i], dbType));
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
     * 构建Upsert SQL
     * - PostgreSQL使用ON CONFLICT
     * - MySQL/Doris使用ON DUPLICATE KEY UPDATE
     */
    public static String buildUpsertSql(String table, String[] columns, String[] primaryKeys, String dbType) {
        String insertSql = buildMultiValueInsertSql(table, columns, 1, dbType);

        if (DB_TYPE_POSTGRESQL.equalsIgnoreCase(dbType)) {
            StringBuilder conflictColumns = new StringBuilder();
            for (int i = 0; i < primaryKeys.length; i++) {
                conflictColumns.append(quoteIdentifier(primaryKeys[i], dbType));
                if (i < primaryKeys.length - 1) {
                    conflictColumns.append(", ");
                }
            }

            StringBuilder updateSet = new StringBuilder();
            for (String column : columns) {
                boolean isPrimaryKey = false;
                for (String pk : primaryKeys) {
                    if (pk.equalsIgnoreCase(column)) {
                        isPrimaryKey = true;
                        break;
                    }
                }
                if (!isPrimaryKey) {
                    String quotedCol = quoteIdentifier(column, dbType);
                    updateSet.append(quotedCol).append(" = EXCLUDED.").append(quotedCol).append(", ");
                }
            }
            if (updateSet.length() > 0) {
                updateSet.setLength(updateSet.length() - 2);
            }

            return insertSql + " ON CONFLICT (" + conflictColumns + ") DO UPDATE SET " + updateSet;
        }

        // MySQL/Doris使用ON DUPLICATE KEY UPDATE
        StringBuilder updateSet = new StringBuilder();
        for (String column : columns) {
            boolean isPrimaryKey = false;
            for (String pk : primaryKeys) {
                if (pk.equalsIgnoreCase(column)) {
                    isPrimaryKey = true;
                    break;
                }
            }
            if (!isPrimaryKey) {
                String quotedCol = quoteIdentifier(column, dbType);
                updateSet.append(quotedCol).append(" = VALUES(").append(quotedCol).append("), ");
            }
        }
        if (updateSet.length() > 0) {
            updateSet.setLength(updateSet.length() - 2);
        }

        return insertSql + " ON DUPLICATE KEY UPDATE " + updateSet;
    }

    /**
     * 构建COUNT查询SQL
     */
    public static String buildCountSql(String table) {
        return buildCountSql(table, DB_TYPE_MYSQL);
    }

    /**
     * 构建近似行数查询SQL
     */
    public static String buildApproximateCountSql(String table) {
        return buildApproximateCountSql(table, DB_TYPE_MYSQL);
    }

    /**
     * 构建批量提取SQL（默认MySQL）
     */
    public static String buildBatchExtractSql(String table, String primaryKey, long lastId, int batchSize) {
        return buildBatchExtractSql(table, primaryKey, lastId, batchSize, DB_TYPE_MYSQL);
    }

    /**
     * 构建增量查询SQL（默认MySQL）
     */
    public static String buildIncrementalQuerySql(String table, String fieldName) {
        return buildIncrementalQuerySql(table, fieldName, DB_TYPE_MYSQL);
    }

    /**
     * 构建多值INSERT SQL（默认MySQL）
     */
    public static String buildMultiValueInsertSql(String table, String[] columns, int rowCount) {
        return buildMultiValueInsertSql(table, columns, rowCount, DB_TYPE_MYSQL);
    }

    /**
     * 构建Upsert SQL（默认MySQL）
     */
    public static String buildUpsertSql(String table, String[] columns, String[] primaryKeys) {
        return buildUpsertSql(table, columns, primaryKeys, DB_TYPE_MYSQL);
    }
}
