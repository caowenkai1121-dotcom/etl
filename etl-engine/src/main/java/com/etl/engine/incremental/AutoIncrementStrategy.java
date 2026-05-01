package com.etl.engine.incremental;

import com.etl.common.domain.SyncPipelineContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;

/**
 * 基于自增主键的增量策略实现
 */
@Component
@Slf4j
public class AutoIncrementStrategy implements IncrementalStrategy {

    @Override
    public String getType() {
        return "AUTO_INCREMENT";
    }

    @Override
    public String buildQuerySql(SyncPipelineContext context, String lastPosition, String dbType) {
        // 构建增量查询SQL - 使用>方式，配合LIMIT批次处理
        StringBuilder sql = new StringBuilder();
        String quotedTable = quoteIdentifier(context.getSourceTable(), dbType);
        String quotedField = quoteIdentifier(context.getIncrementalField(), dbType);

        sql.append("SELECT ");
        if (context.getSourceColumns() != null && !context.getSourceColumns().isEmpty()) {
            String[] columns = context.getSourceColumns().split(",");
            for (int i = 0; i < columns.length; i++) {
                if (i > 0) sql.append(", ");
                sql.append(quoteIdentifier(columns[i].trim(), dbType));
            }
        } else {
            sql.append("*");
        }

        sql.append(" FROM ").append(quotedTable);
        sql.append(" WHERE ").append(quotedField).append(" > ");
        // 处理值的引号
        if (isNumericValue(lastPosition)) {
            sql.append(lastPosition);
        } else {
            sql.append("'").append(escapeSql(lastPosition)).append("'");
        }
        sql.append(" ORDER BY ").append(quotedField);
        sql.append(" LIMIT ").append(context.getBatchSize());

        return sql.toString();
    }

    @Override
    public String extractPosition(ResultSet rs, String fieldName) throws Exception {
        Object value = rs.getObject(fieldName);
        if (value == null) {
            return "";
        }
        return value.toString();
    }

    @Override
    public int comparePosition(String pos1, String pos2) {
        try {
            // 支持数值比较
            if (isNumericValue(pos1) && isNumericValue(pos2)) {
                return Long.compare(Long.parseLong(pos1), Long.parseLong(pos2));
            }
        } catch (Exception e) {
            log.warn("位置值转换为数字失败，使用字符串比较: pos1={}, pos2={}", pos1, pos2, e);
        }
        // 使用字符串比较
        return pos1.compareTo(pos2);
    }

    @Override
    public boolean supportsCheckpoint() {
        return true;
    }

    private boolean isNumericValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        String str = value.trim();
        return str.matches("\\d+");
    }

    private String quoteIdentifier(String identifier, String dbType) {
        if (identifier == null || identifier.isEmpty()) {
            return "";
        }
        if ("POSTGRESQL".equalsIgnoreCase(dbType) || "ORACLE".equalsIgnoreCase(dbType)) {
            return "\"" + identifier + "\"";
        } else if ("SQLSERVER".equalsIgnoreCase(dbType)) {
            return "[" + identifier + "]";
        }
        return "`" + identifier + "`";
    }

    private String escapeSql(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("'", "''");
    }
}
