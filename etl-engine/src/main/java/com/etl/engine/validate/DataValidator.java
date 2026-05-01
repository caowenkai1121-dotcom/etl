package com.etl.engine.validate;

import com.etl.common.domain.ColumnInfo;
import com.etl.common.domain.TableInfo;
import com.etl.datasource.connector.DatabaseConnector;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

/**
 * 数据校验器
 * 用于验证源表与目标表数据一致性
 */
@Slf4j
public class DataValidator {

    private final DatabaseConnector sourceConnector;
    private final DatabaseConnector targetConnector;
    private final double sampleRate; // 抽样比例 (0.0 - 1.0)

    public DataValidator(DatabaseConnector sourceConnector, DatabaseConnector targetConnector) {
        this(sourceConnector, targetConnector, 0.1); // 默认抽样 10%
    }

    public DataValidator(DatabaseConnector sourceConnector, DatabaseConnector targetConnector, double sampleRate) {
        this.sourceConnector = sourceConnector;
        this.targetConnector = targetConnector;
        this.sampleRate = Math.min(1.0, Math.max(0.0, sampleRate));
    }

    /**
     * 执行完整校验
     */
    public ValidationResult validate(String sourceTable, String targetTable, List<String> primaryKeyColumns) {
        ValidationResult result = new ValidationResult();
        result.setSourceTable(sourceTable);
        result.setTargetTable(targetTable);
        result.setValidateTime(new Date());

        try {
            // 1. 行数校验
            log.info("开始行数校验: {} -> {}", sourceTable, targetTable);
            CountResult countResult = validateRowCount(sourceTable, targetTable);
            result.setSourceCount(countResult.getSourceCount());
            result.setTargetCount(countResult.getTargetCount());
            result.setCountMatch(countResult.isMatch());
            result.setCountDiff(Math.abs(countResult.getSourceCount() - countResult.getTargetCount()));

            // 2. 数据抽样校验
            log.info("开始数据抽样校验: {} -> {}", sourceTable, targetTable);
            SampleResult sampleResult = validateSample(sourceTable, targetTable, primaryKeyColumns);
            result.setSampleSize(sampleResult.getSampleSize());
            result.setMatchCount(sampleResult.getMatchCount());
            result.setMismatchCount(sampleResult.getMismatchCount());
            result.setSampleMatchRate(sampleResult.getMatchRate());

            // 3. 主键一致性校验
            log.info("开始主键一致性校验: {} -> {}", sourceTable, targetTable);
            KeyConsistencyResult keyResult = validateKeyConsistency(sourceTable, targetTable, primaryKeyColumns);
            result.setMissingKeys(keyResult.getMissingKeys());
            result.setExtraKeys(keyResult.getExtraKeys());

            // 计算整体通过率
            boolean countPassed = countResult.isMatch();
            boolean samplePassed = sampleResult.getMatchRate() >= 0.99; // 99% 通过率
            boolean keyPassed = keyResult.getMissingKeys().isEmpty() && keyResult.getExtraKeys().isEmpty();

            result.setPassed(countPassed && samplePassed && keyPassed);
            result.setSummary(generateSummary(result));

        } catch (Exception e) {
            log.error("数据校验失败: {} -> {}", sourceTable, targetTable, e);
            result.setPassed(false);
            result.setError(e.getMessage());
        }

        return result;
    }

    /**
     * 行数校验
     */
    private CountResult validateRowCount(String sourceTable, String targetTable) throws Exception {
        CountResult result = new CountResult();

        long sourceCount = getRowCount(sourceConnector, sourceTable);
        long targetCount = getRowCount(targetConnector, targetTable);

        result.setSourceCount(sourceCount);
        result.setTargetCount(targetCount);
        result.setMatch(sourceCount == targetCount);

        log.info("行数校验结果: 源表={}, 目标表={}, 匹配={}", sourceCount, targetCount, result.isMatch());
        return result;
    }

    /**
     * 获取表行数
     */
    private long getRowCount(DatabaseConnector connector, String tableName) throws Exception {
        String quotedTable = quoteIdentifier(connector, tableName);
        String sql = "SELECT COUNT(*) FROM " + quotedTable;

        try (Connection conn = connector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        }
        return 0;
    }

    /**
     * 数据抽样校验
     */
    private SampleResult validateSample(String sourceTable, String targetTable, List<String> primaryKeyColumns) throws Exception {
        SampleResult result = new SampleResult();

        // 获取源表结构
        TableInfo sourceTableInfo = sourceConnector.getTableInfo(sourceTable);
        if (sourceTableInfo == null) {
            throw new RuntimeException("源表不存在: " + sourceTable);
        }

        List<ColumnInfo> columns = sourceTableInfo.getColumns();
        if (columns.isEmpty()) {
            return result;
        }

        // 构建主键条件
        String pkColumns = String.join(", ", primaryKeyColumns.stream()
            .map(c -> quoteColumn(sourceConnector, c))
            .toList());

        // 随机抽样
        String sourceQuoted = quoteIdentifier(sourceConnector, sourceTable);
        String targetQuoted = quoteIdentifier(targetConnector, targetTable);

        // 使用 RANDOM() 函数进行随机抽样（不同数据库语法不同）
        String sampleSql = buildSampleSql(sourceConnector, sourceQuoted, pkColumns, columns);

        int sampleSize = 0;
        int matchCount = 0;
        int mismatchCount = 0;

        try (Connection sourceConn = sourceConnector.getConnection();
             Connection targetConn = targetConnector.getConnection();
             PreparedStatement sourceStmt = sourceConn.prepareStatement(sampleSql);
             ResultSet sourceRs = sourceStmt.executeQuery()) {

            while (sourceRs.next()) {
                sampleSize++;

                // 构建 WHERE 条件
                StringBuilder whereClause = new StringBuilder();
                List<Object> params = new ArrayList<>();

                for (int i = 0; i < primaryKeyColumns.size(); i++) {
                    if (i > 0) whereClause.append(" AND ");
                    String pkCol = primaryKeyColumns.get(i);
                    whereClause.append(quoteColumn(targetConnector, pkCol)).append(" = ?");
                    params.add(sourceRs.getObject(pkCol));
                }

                // 查询目标表对应记录
                String targetQuery = buildTargetQuery(targetQuoted, columns, whereClause.toString());

                try (PreparedStatement targetStmt = targetConn.prepareStatement(targetQuery)) {
                    for (int i = 0; i < params.size(); i++) {
                        targetStmt.setObject(i + 1, params.get(i));
                    }

                    try (ResultSet targetRs = targetStmt.executeQuery()) {
                        if (targetRs.next()) {
                            // 比较字段值
                            boolean match = compareRow(sourceRs, targetRs, columns);
                            if (match) {
                                matchCount++;
                            } else {
                                mismatchCount++;
                            }
                        } else {
                            // 目标表缺少此记录
                            mismatchCount++;
                        }
                    }
                }
            }
        }

        result.setSampleSize(sampleSize);
        result.setMatchCount(matchCount);
        result.setMismatchCount(mismatchCount);
        if (sampleSize > 0) {
            result.setMatchRate((double) matchCount / sampleSize);
        }

        log.info("抽样校验结果: 样本数={}, 匹配={}, 不匹配={}, 匹配率={}",
            sampleSize, matchCount, mismatchCount, result.getMatchRate());

        return result;
    }

    /**
     * 构建抽样SQL
     */
    private String buildSampleSql(DatabaseConnector connector, String tableName, String pkColumns, List<ColumnInfo> columns) {
        String dbType = connector.getDatabaseType();
        String allColumns = String.join(", ", columns.stream()
            .map(c -> quoteColumn(connector, c.getColumnName()))
            .toList());

        if ("MYSQL".equals(dbType)) {
            return String.format("SELECT %s FROM %s ORDER BY RAND() LIMIT %d",
                allColumns, tableName, (int)(1000 * sampleRate));
        } else if ("POSTGRESQL".equals(dbType)) {
            return String.format("SELECT %s FROM %s ORDER BY RANDOM() LIMIT %d",
                allColumns, tableName, (int)(1000 * sampleRate));
        } else if ("DORIS".equals(dbType)) {
            return String.format("SELECT %s FROM %s TABLESAMPLE(%d ROWS)",
                allColumns, tableName, (int)(1000 * sampleRate));
        }

        return String.format("SELECT %s FROM %s", allColumns, tableName);
    }

    /**
     * 构建目标表查询
     */
    private String buildTargetQuery(String tableName, List<ColumnInfo> columns, String whereClause) {
        String allColumns = String.join(", ", columns.stream()
            .map(c -> quoteColumn(targetConnector, c.getColumnName()))
            .toList());

        return String.format("SELECT %s FROM %s WHERE %s", allColumns, tableName, whereClause);
    }

    /**
     * 比较两行数据
     */
    private boolean compareRow(ResultSet sourceRs, ResultSet targetRs, List<ColumnInfo> columns) throws Exception {
        for (ColumnInfo column : columns) {
            String colName = column.getColumnName();
            Object sourceValue = sourceRs.getObject(colName);
            Object targetValue = targetRs.getObject(colName);

            if (!Objects.equals(normalizeValue(sourceValue), normalizeValue(targetValue))) {
                log.debug("字段值不匹配: {} (源={}, 目标={})", colName, sourceValue, targetValue);
                return false;
            }
        }
        return true;
    }

    /**
     * 标准化值（处理类型差异）
     */
    private Object normalizeValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal) {
            return ((BigDecimal) value).stripTrailingZeros().toPlainString();
        }
        if (value instanceof Number) {
            return value.toString();
        }
        return value;
    }

    /**
     * 主键一致性校验
     */
    private KeyConsistencyResult validateKeyConsistency(String sourceTable, String targetTable, List<String> primaryKeyColumns) throws Exception {
        KeyConsistencyResult result = new KeyConsistencyResult();
        result.setMissingKeys(new ArrayList<>());
        result.setExtraKeys(new ArrayList<>());

        if (primaryKeyColumns == null || primaryKeyColumns.isEmpty()) {
            return result;
        }

        String pkColumns = String.join(", ", primaryKeyColumns.stream()
            .map(c -> quoteColumn(sourceConnector, c))
            .toList());

        // 获取源表所有主键
        Set<String> sourceKeys = new HashSet<>();
        String sourceQuoted = quoteIdentifier(sourceConnector, sourceTable);
        String sourceKeySql = String.format("SELECT %s FROM %s", pkColumns, sourceQuoted);

        try (Connection conn = sourceConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sourceKeySql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                sourceKeys.add(buildKeyString(rs, primaryKeyColumns));
            }
        }

        // 获取目标表所有主键
        Set<String> targetKeys = new HashSet<>();
        String targetQuoted = quoteIdentifier(targetConnector, targetTable);
        String targetKeySql = String.format("SELECT %s FROM %s", pkColumns, targetQuoted);

        try (Connection conn = targetConnector.getConnection();
             PreparedStatement stmt = conn.prepareStatement(targetKeySql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                targetKeys.add(buildKeyString(rs, primaryKeyColumns));
            }
        }

        // 找出缺失的主键（源表有，目标表没有）
        for (String key : sourceKeys) {
            if (!targetKeys.contains(key)) {
                result.getMissingKeys().add(key);
            }
        }

        // 找出多余的主键（目标表有，源表没有）
        for (String key : targetKeys) {
            if (!sourceKeys.contains(key)) {
                result.getExtraKeys().add(key);
            }
        }

        log.info("主键一致性校验结果: 缺失={}, 多余={}", result.getMissingKeys().size(), result.getExtraKeys().size());
        return result;
    }

    /**
     * 构建主键字符串
     */
    private String buildKeyString(ResultSet rs, List<String> primaryKeyColumns) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < primaryKeyColumns.size(); i++) {
            if (i > 0) sb.append("|");
            Object value = rs.getObject(primaryKeyColumns.get(i));
            sb.append(value != null ? value.toString() : "NULL");
        }
        return sb.toString();
    }

    /**
     * 引用标识符
     */
    private String quoteIdentifier(DatabaseConnector connector, String identifier) {
        if ("POSTGRESQL".equals(connector.getDatabaseType())) {
            return "\"" + identifier + "\"";
        }
        return "`" + identifier + "`";
    }

    /**
     * 引用列名
     */
    private String quoteColumn(DatabaseConnector connector, String columnName) {
        if ("POSTGRESQL".equals(connector.getDatabaseType())) {
            return "\"" + columnName + "\"";
        } else if ("DORIS".equals(connector.getDatabaseType())) {
            return "`" + columnName + "`";
        }
        return "`" + columnName + "`";
    }

    /**
     * 生成校验摘要
     */
    private String generateSummary(ValidationResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("数据校验报告: ").append(result.getSourceTable()).append(" -> ").append(result.getTargetTable()).append("\n");
        sb.append("校验时间: ").append(result.getValidateTime()).append("\n");
        sb.append("校验结果: ").append(result.isPassed() ? "通过" : "失败").append("\n");
        sb.append("行数对比: 源=").append(result.getSourceCount()).append(", 目标=").append(result.getTargetCount());
        sb.append(", 差异=").append(result.getCountDiff()).append("\n");
        sb.append("抽样校验: 样本数=").append(result.getSampleSize());
        sb.append(", 匹配=").append(result.getMatchCount());
        sb.append(", 不匹配=").append(result.getMismatchCount());
        sb.append(", 匹配率=").append(String.format("%.2f%%", result.getSampleMatchRate() * 100)).append("\n");

        if (!result.getMissingKeys().isEmpty()) {
            sb.append("缺失主键: ").append(result.getMissingKeys().size()).append("条\n");
        }
        if (!result.getExtraKeys().isEmpty()) {
            sb.append("多余主键: ").append(result.getExtraKeys().size()).append("条\n");
        }

        return sb.toString();
    }

    // ==================== 内部类 ====================

    @Data
    public static class ValidationResult {
        private String sourceTable;
        private String targetTable;
        private Date validateTime;
        private boolean passed;
        private String error;

        // 行数校验
        private long sourceCount;
        private long targetCount;
        private boolean countMatch;
        private long countDiff;

        // 抽样校验
        private int sampleSize;
        private int matchCount;
        private int mismatchCount;
        private double sampleMatchRate;

        // 主键一致性
        private List<String> missingKeys;
        private List<String> extraKeys;

        private String summary;
    }

    @Data
    private static class CountResult {
        private long sourceCount;
        private long targetCount;
        private boolean match;
    }

    @Data
    private static class SampleResult {
        private int sampleSize;
        private int matchCount;
        private int mismatchCount;
        private double matchRate;
    }

    @Data
    private static class KeyConsistencyResult {
        private List<String> missingKeys;
        private List<String> extraKeys;
    }
}
