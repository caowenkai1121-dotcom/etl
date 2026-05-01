package com.etl.engine.schema;

import com.etl.common.domain.ColumnInfo;
import com.etl.common.domain.TableInfo;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 数据库类型映射服务实现
 */
@Service
public class TypeMappingServiceImpl implements TypeMappingService {

    /**
     * 类型映射表 key: sourceDbType_targetDbType
     */
    private static final Map<String, Map<String, String>> TYPE_MAPPINGS = new HashMap<>();

    static {
        // MySQL → PostgreSQL
        Map<String, String> mysqlToPg = new HashMap<>();
        mysqlToPg.put("TINYINT", "SMALLINT");
        mysqlToPg.put("INT", "INTEGER");
        mysqlToPg.put("BIGINT", "BIGINT");
        mysqlToPg.put("DATETIME", "TIMESTAMP");
        mysqlToPg.put("TEXT", "TEXT");
        mysqlToPg.put("MEDIUMTEXT", "TEXT");
        mysqlToPg.put("LONGTEXT", "TEXT");
        mysqlToPg.put("BLOB", "BYTEA");
        mysqlToPg.put("JSON", "JSONB");
        mysqlToPg.put("AUTO_INCREMENT", "SERIAL");
        mysqlToPg.put("BOOLEAN", "BOOLEAN");
        mysqlToPg.put("TIMESTAMP", "TIMESTAMP");
        mysqlToPg.put("VARCHAR", "VARCHAR");
        mysqlToPg.put("CHAR", "CHAR");
        mysqlToPg.put("FLOAT", "REAL");
        mysqlToPg.put("DOUBLE", "DOUBLE PRECISION");
        mysqlToPg.put("DECIMAL", "NUMERIC");
        mysqlToPg.put("DATE", "DATE");
        mysqlToPg.put("TIME", "TIME");
        TYPE_MAPPINGS.put("MYSQL_POSTGRESQL", mysqlToPg);

        // MySQL → Doris
        Map<String, String> mysqlToDoris = new HashMap<>();
        mysqlToDoris.put("TINYINT", "TINYINT");
        mysqlToDoris.put("INT", "INT");
        mysqlToDoris.put("BIGINT", "BIGINT");
        mysqlToDoris.put("DATETIME", "DATETIMEV2(3)");
        mysqlToDoris.put("TIMESTAMP", "DATETIMEV2(6)");
        mysqlToDoris.put("TEXT", "STRING");
        mysqlToDoris.put("MEDIUMTEXT", "STRING");
        mysqlToDoris.put("LONGTEXT", "STRING");
        mysqlToDoris.put("BLOB", "STRING");
        mysqlToDoris.put("JSON", "JSON");
        mysqlToDoris.put("AUTO_INCREMENT", "");
        mysqlToDoris.put("BOOLEAN", "BOOLEAN");
        mysqlToDoris.put("VARCHAR", "VARCHAR");
        mysqlToDoris.put("CHAR", "CHAR");
        mysqlToDoris.put("FLOAT", "FLOAT");
        mysqlToDoris.put("DOUBLE", "DOUBLE");
        mysqlToDoris.put("DECIMAL", "DECIMAL");
        mysqlToDoris.put("DATE", "DATE");
        mysqlToDoris.put("TIME", "VARCHAR(20)");
        TYPE_MAPPINGS.put("MYSQL_DORIS", mysqlToDoris);

        // PostgreSQL → MySQL
        Map<String, String> pgToMysql = new HashMap<>();
        pgToMysql.put("INTEGER", "INT");
        pgToMysql.put("SERIAL", "INT AUTO_INCREMENT");
        pgToMysql.put("TIMESTAMP", "DATETIME");
        pgToMysql.put("JSONB", "JSON");
        pgToMysql.put("BYTEA", "BLOB");
        pgToMysql.put("REAL", "FLOAT");
        pgToMysql.put("DOUBLE PRECISION", "DOUBLE");
        pgToMysql.put("NUMERIC", "DECIMAL");
        pgToMysql.put("BOOLEAN", "BOOLEAN");
        pgToMysql.put("VARCHAR", "VARCHAR");
        pgToMysql.put("TEXT", "TEXT");
        pgToMysql.put("CHAR", "CHAR");
        pgToMysql.put("DATE", "DATE");
        pgToMysql.put("TIME", "TIME");
        pgToMysql.put("BIGINT", "BIGINT");
        TYPE_MAPPINGS.put("POSTGRESQL_MYSQL", pgToMysql);
    }

    @Override
    public String mapType(String sourceDbType, String sourceType, String targetDbType) {
        // 参数校验
        if (sourceType == null) {
            return null;
        }
        // 如果targetDbType为空，直接返回原类型
        if (targetDbType == null) {
            return sourceType;
        }
        // 同类型数据库直接返回原类型
        if (sourceDbType != null && sourceDbType.equalsIgnoreCase(targetDbType)) {
            return sourceType;
        }
        // 如果sourceDbType为空，直接返回原类型
        if (sourceDbType == null) {
            return sourceType;
        }

        // 获取对应的类型映射表
        String key = sourceDbType.toUpperCase() + "_" + targetDbType.toUpperCase();
        Map<String, String> mappings = TYPE_MAPPINGS.get(key);

        // 如果没有找到对应的映射表，返回原类型
        if (mappings == null) {
            return sourceType;
        }

        // 查找对应的类型映射，不区分大小写
        String mappedType = mappings.get(sourceType.toUpperCase());

        // 如果没有找到对应的类型映射，返回原类型
        return mappedType != null ? mappedType : sourceType;
    }

    @Override
    public String getCreateTableDialect(String dbType, TableInfo tableInfo, CreateConfig config) {
        dbType = dbType.toUpperCase();
        StringBuilder sql = new StringBuilder();

        // 根据数据库类型选择引号
        String leftQuote, rightQuote;
        if (dbType.equals("POSTGRESQL")) {
            leftQuote = rightQuote = "\"";
        } else {
            leftQuote = rightQuote = "`";
        }

        sql.append("CREATE TABLE ").append(leftQuote).append(tableInfo.getTableName()).append(rightQuote).append(" (\n");

        // 添加字段
        for (int i = 0; i < tableInfo.getColumns().size(); i++) {
            ColumnInfo column = tableInfo.getColumns().get(i);
            String columnType = mapType(null, column.getColumnType(), dbType);

            sql.append("  ").append(leftQuote).append(column.getColumnName()).append(rightQuote);
            sql.append(" ").append(columnType);

            // 添加列长度/精度（VARCHAR, CHAR, DECIMAL等需要）
            if (needsLength(columnType) && column.getColumnLength() != null && column.getColumnLength() > 0) {
                if (columnType.toUpperCase().contains("DECIMAL") || columnType.toUpperCase().contains("NUMERIC")) {
                    int precision = column.getColumnLength();
                    int scale = column.getDecimalDigits() != null ? column.getDecimalDigits() : 0;
                    sql.append("(").append(precision).append(", ").append(scale).append(")");
                } else {
                    sql.append("(").append(column.getColumnLength());
                    if (column.getDecimalDigits() != null && column.getDecimalDigits() > 0) {
                        sql.append(", ").append(column.getDecimalDigits());
                    }
                    sql.append(")");
                }
            } else if (needsDefaultLength(columnType)) {
                // 为没有长度信息的字符类型添加默认长度
                sql.append("(").append(getDefaultLength(columnType)).append(")");
            }

            // 处理 SERIAL 类型（不需要长度）
            if (column.getAutoIncrement() != null && column.getAutoIncrement()) {
                if (dbType.equals("POSTGRESQL")) {
                    sql.append(" SERIAL");
                } else if (dbType.equals("SQLSERVER")) {
                    sql.append(" IDENTITY(1,1)");
                } else if (!dbType.equals("DORIS")) {
                    sql.append(" AUTO_INCREMENT");
                }
            }

            if (Boolean.FALSE.equals(column.getNullable())) {
                sql.append(" NOT NULL");
            }

            if (column.getColumnComment() != null && !column.getColumnComment().isEmpty()) {
                sql.append(" COMMENT '").append(escapeComment(column.getColumnComment())).append("'");
            }

            if (i < tableInfo.getColumns().size() - 1 || (config.getPrimaryKeys() != null && config.getPrimaryKeys().length > 0)) {
                sql.append(",");
            }
            sql.append("\n");
        }

        // 添加主键约束
        if (config.getPrimaryKeys() != null && config.getPrimaryKeys().length > 0) {
            if (dbType.equals("DORIS")) {
                // Doris 使用 UNIQUE KEY 模型
                sql.append("  UNIQUE KEY (");
            } else {
                sql.append("  PRIMARY KEY (");
            }

            for (int i = 0; i < config.getPrimaryKeys().length; i++) {
                if (i > 0) {
                    sql.append(", ");
                }
                sql.append(leftQuote).append(config.getPrimaryKeys()[i]).append(rightQuote);
            }
            sql.append(")\n");
        }

        sql.append(")");

        // 添加表注释
        if (tableInfo.getTableComment() != null && !tableInfo.getTableComment().isEmpty()) {
            if (dbType.equals("POSTGRESQL")) {
                sql.append(";\nCOMMENT ON TABLE ").append(leftQuote).append(tableInfo.getTableName()).append(rightQuote)
                        .append(" IS '").append(escapeComment(tableInfo.getTableComment())).append("'");
            } else {
                sql.append(" COMMENT='").append(escapeComment(tableInfo.getTableComment())).append("'");
            }
        }

        // 添加 Doris 分布式配置
        if (dbType.equals("DORIS")) {
            sql.append("\nDISTRIBUTED BY HASH(");
            if (config.getDistributionKey() != null) {
                sql.append(leftQuote).append(config.getDistributionKey()).append(rightQuote);
            } else if (config.getPrimaryKeys() != null && config.getPrimaryKeys().length > 0) {
                sql.append(leftQuote).append(config.getPrimaryKeys()[0]).append(rightQuote);
            } else {
                sql.append(leftQuote).append(tableInfo.getColumns().get(0).getColumnName()).append(rightQuote);
            }
            sql.append(") BUCKETS ").append(config.getBuckets());

            sql.append("\nPROPERTIES (\n");
            sql.append("  \"replication_num\" = \"").append(config.getReplicationNum()).append("\"\n");
            sql.append(")");
        }

        return sql.toString();
    }

    private String escapeComment(String comment) {
        return comment.replace("'", "\\'");
    }

    /**
     * 判断类型是否需要指定长度
     */
    private boolean needsLength(String columnType) {
        if (columnType == null) return false;
        String upperType = columnType.toUpperCase();
        return upperType.contains("VARCHAR") || upperType.contains("CHAR") ||
               upperType.contains("DECIMAL") || upperType.contains("NUMERIC");
    }

    /**
     * 判断类型是否需要默认长度（当没有指定长度时）
     */
    private boolean needsDefaultLength(String columnType) {
        if (columnType == null) return false;
        String upperType = columnType.toUpperCase();
        // VARCHAR和CHAR必须指定长度
        return upperType.equals("VARCHAR") || upperType.equals("CHAR") ||
               upperType.equals("NVARCHAR") || upperType.equals("NCHAR");
    }

    /**
     * 获取类型的默认长度
     */
    private int getDefaultLength(String columnType) {
        if (columnType == null) return 255;
        String upperType = columnType.toUpperCase();
        if (upperType.equals("CHAR") || upperType.equals("NCHAR")) {
            return 1;
        }
        return 255; // VARCHAR默认长度
    }
}
