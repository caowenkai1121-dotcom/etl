package com.etl.engine.validate;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 数据质量规则引擎
 * 对数据进行质量校验，识别异常数据
 */
@Slf4j
public class DataQualityEngine {

    private final List<QualityRule> rules = new ArrayList<>();

    public DataQualityEngine addRule(QualityRule rule) {
        this.rules.add(rule);
        return this;
    }

    /**
     * 对一批数据进行质量校验
     *
     * @return 校验结果列表
     */
    public List<QualityCheckResult> validate(List<Map<String, Object>> data) {
        List<QualityCheckResult> results = new ArrayList<>();
        for (QualityRule rule : rules) {
            if (!rule.isEnabled()) continue;
            try {
                List<QualityCheckResult> ruleResults = applyRule(rule, data);
                results.addAll(ruleResults);
            } catch (Exception e) {
                log.error("质量规则[{}]执行失败", rule.getName(), e);
            }
        }
        return results;
    }

    private List<QualityCheckResult> applyRule(QualityRule rule, List<Map<String, Object>> data) {
        List<QualityCheckResult> results = new ArrayList<>();

        switch (rule.getType()) {
            case NOT_NULL:
                for (int i = 0; i < data.size(); i++) {
                    Object val = data.get(i).get(rule.getFieldName());
                    if (val == null || "".equals(val)) {
                        results.add(fail(rule, i, data.get(i), val, "非空"));
                    }
                }
                break;

            case UNIQUE:
                Set<Object> seen = new HashSet<>();
                for (int i = 0; i < data.size(); i++) {
                    Object val = data.get(i).get(rule.getFieldName());
                    if (val != null && !seen.add(val)) {
                        results.add(fail(rule, i, data.get(i), val, "唯一性"));
                    }
                }
                break;

            case DATA_TYPE:
                Pattern typePattern = getTypePattern(rule.getExpectedValue());
                for (int i = 0; i < data.size(); i++) {
                    Object val = data.get(i).get(rule.getFieldName());
                    if (val != null && !"".equals(val) && typePattern != null && !typePattern.matcher(String.valueOf(val)).matches()) {
                        results.add(fail(rule, i, data.get(i), val, "类型:" + rule.getExpectedValue()));
                    }
                }
                break;

            case RANGE_VALUE:
                Double min = rule.getMinValue() != null ? Double.parseDouble(String.valueOf(rule.getMinValue())) : null;
                Double max = rule.getMaxValue() != null ? Double.parseDouble(String.valueOf(rule.getMaxValue())) : null;
                for (int i = 0; i < data.size(); i++) {
                    Object val = data.get(i).get(rule.getFieldName());
                    if (val == null || "".equals(val)) continue;
                    try {
                        double numVal = Double.parseDouble(String.valueOf(val));
                        if ((min != null && numVal < min) || (max != null && numVal > max)) {
                            results.add(fail(rule, i, data.get(i), val, "范围[" + min + "," + max + "]"));
                        }
                    } catch (NumberFormatException ignored) {}
                }
                break;

            case CUSTOM:
                // 自定义脚本规则 - 简化实现
                if (rule.getScriptExpression() != null) {
                    for (int i = 0; i < data.size(); i++) {
                        // 简单的表达式校验
                        boolean passed = evaluateCustomRule(rule.getScriptExpression(), data.get(i));
                        if (!passed) {
                            results.add(fail(rule, i, data.get(i), data.get(i).get(rule.getFieldName()), "自定义规则"));
                        }
                    }
                }
                break;
        }

        return results;
    }

    private QualityCheckResult fail(QualityRule rule, int rowIndex, Map<String, Object> row, Object actualValue, String expectedDesc) {
        QualityCheckResult result = new QualityCheckResult();
        result.setRuleName(rule.getName());
        result.setRuleType(rule.getType().name());
        result.setFieldName(rule.getFieldName());
        result.setFieldValue(String.valueOf(actualValue));
        result.setExpectedValue(expectedDesc);
        result.setActualValue(String.valueOf(actualValue));
        result.setSeverity(rule.getSeverity());
        result.setRowIndex(rowIndex);
        return result;
    }

    private Pattern getTypePattern(String type) {
        if (type == null) return null;
        switch (type.toUpperCase()) {
            case "EMAIL": return Pattern.compile("^[\\w.-]+@[\\w.-]+\\.\\w{2,}$");
            case "PHONE": return Pattern.compile("^1[3-9]\\d{9}$");
            case "URL": return Pattern.compile("^https?://.+");
            case "IP": return Pattern.compile("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$");
            case "DATE": return Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");
            case "DATETIME": return Pattern.compile("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}$");
            case "NUMERIC": return Pattern.compile("^-?\\d+(\\.\\d+)?$");
            case "INTEGER": return Pattern.compile("^-?\\d+$");
            default: return null;
        }
    }

    private boolean evaluateCustomRule(String expression, Map<String, Object> row) {
        // 简单表达式: ${fieldName} != null && ${fieldName} > 0
        try {
            String expr = expression;
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                String val = entry.getValue() != null ? entry.getValue().toString() : "null";
                expr = expr.replace("${" + entry.getKey() + "}", val);
            }
            // 使用 ScriptEngine 执行表达式
            return true; // 简化实现
        } catch (Exception e) {
            return false;
        }
    }

    // ===== 内部类 =====

    @Data
    public static class QualityRule {
        private Long id;
        private String name;
        private QualityRuleType type = QualityRuleType.NOT_NULL;
        private String fieldName;
        private String expectedValue;
        private Object minValue;
        private Object maxValue;
        private String scriptExpression;
        private String severity = "WARNING";
        private boolean enabled = true;

        public enum QualityRuleType {
            NOT_NULL("非空校验"),
            UNIQUE("唯一性校验"),
            DATA_TYPE("数据类型校验"),
            RANGE_VALUE("值域校验"),
            CUSTOM("自定义校验");

            private final String description;
            QualityRuleType(String description) { this.description = description; }
        }
    }

    @Data
    public static class QualityCheckResult {
        private String ruleName;
        private String ruleType;
        private String fieldName;
        private String fieldValue;
        private String expectedValue;
        private String actualValue;
        private String severity = "WARNING";
        private int rowIndex;
        private boolean passed;
    }
}
