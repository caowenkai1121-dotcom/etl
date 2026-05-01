package com.etl.engine.transform;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 数据转换器
 * 用于数据清洗和转换
 */
@Slf4j
@Component
public class DataTransformer {

    /**
     * 应用转换规则
     *
     * @param data   原始数据
     * @param rules  转换规则列表
     * @return 转换后的数据
     */
    public Map<String, Object> transform(Map<String, Object> data, List<TransformRule> rules) {
        if (rules == null || rules.isEmpty()) {
            return data;
        }

        Map<String, Object> result = new HashMap<>(data);

        for (TransformRule rule : rules) {
            if (!rule.isEnabled()) {
                continue;
            }

            try {
                result = applyRule(result, rule);
            } catch (Exception e) {
                log.error("应用转换规则失败: rule={}", rule.getName(), e);
                if (rule.isStopOnError()) {
                    throw new RuntimeException("转换规则执行失败: " + rule.getName(), e);
                }
            }
        }

        return result;
    }

    /**
     * 批量转换
     */
    public List<Map<String, Object>> transformBatch(List<Map<String, Object>> dataList, List<TransformRule> rules) {
        List<Map<String, Object>> result = new ArrayList<>();

        for (Map<String, Object> data : dataList) {
            try {
                Map<String, Object> transformed = transform(data, rules);
                // 检查过滤条件
                if (!isFiltered(transformed, rules)) {
                    result.add(transformed);
                }
            } catch (Exception e) {
                log.error("数据转换失败", e);
            }
        }

        return result;
    }

    /**
     * 应用单个转换规则
     */
    private Map<String, Object> applyRule(Map<String, Object> data, TransformRule rule) {
        String fieldName = rule.getFieldName();
        Object originalValue = data.get(fieldName);

        switch (rule.getType()) {
            case VALUE_MAP:
                // 值映射
                data.put(fieldName, applyValueMap(originalValue, rule.getValueMapping()));
                break;

            case FORMAT_CONVERT:
                // 格式转换
                data.put(fieldName, applyFormatConvert(originalValue, rule.getFormatPattern()));
                break;

            case EXPRESSION:
                // 表达式计算
                Object computedValue = evaluateExpression(rule.getExpression(), data);
                if (rule.getTargetField() != null) {
                    data.put(rule.getTargetField(), computedValue);
                } else {
                    data.put(fieldName, computedValue);
                }
                break;

            case FIELD_CONCAT:
                // 字段拼接
                String concatenated = applyFieldConcat(data, rule.getSourceFields(), rule.getSeparator());
                data.put(rule.getTargetField(), concatenated);
                break;

            case DEFAULT_VALUE:
                // 默认值
                if (originalValue == null || "".equals(originalValue)) {
                    data.put(fieldName, rule.getDefaultValue());
                }
                break;

            case TRIM:
                // 去除空白
                if (originalValue instanceof String) {
                    data.put(fieldName, ((String) originalValue).trim());
                }
                break;

            case UPPER_CASE:
                // 转大写
                if (originalValue instanceof String) {
                    data.put(fieldName, ((String) originalValue).toUpperCase());
                }
                break;

            case LOWER_CASE:
                // 转小写
                if (originalValue instanceof String) {
                    data.put(fieldName, ((String) originalValue).toLowerCase());
                }
                break;

            case REGEX_REPLACE:
                // 正则替换
                if (originalValue instanceof String) {
                    String replaced = applyRegexReplace((String) originalValue, rule.getRegexPattern(), rule.getReplacement());
                    data.put(fieldName, replaced);
                }
                break;

            case FIELD_RENAME:
                // 字段重命名
                if (data.containsKey(fieldName)) {
                    data.put(rule.getTargetField(), data.remove(fieldName));
                }
                break;

            case FIELD_ADD:
                // 添加字段
                data.put(rule.getTargetField(), rule.getDefaultValue());
                break;

            case FIELD_REMOVE:
                // 移除字段
                data.remove(fieldName);
                break;

            default:
                log.warn("未知的转换类型: {}", rule.getType());
        }

        return data;
    }

    /**
     * 值映射
     */
    private Object applyValueMap(Object value, Map<String, Object> mapping) {
        if (mapping == null || value == null) {
            return value;
        }

        String key = String.valueOf(value);
        return mapping.getOrDefault(key, value);
    }

    /**
     * 格式转换
     */
    private Object applyFormatConvert(Object value, String pattern) {
        if (value == null || pattern == null) {
            return value;
        }

        // 日期格式转换
        if (value instanceof Date && pattern != null) {
            return new java.text.SimpleDateFormat(pattern).format((Date) value);
        }

        // 数字格式转换
        if (value instanceof Number && pattern != null) {
            return new java.text.DecimalFormat(pattern).format(value);
        }

        return value;
    }

    /**
     * 表达式计算
     */
    private Object evaluateExpression(String expression, Map<String, Object> data) {
        if (expression == null || expression.isEmpty()) {
            return null;
        }

        // 简单表达式解析（支持基本运算和字段引用）
        String expr = expression;

        // 替换字段引用 ${fieldName}
        Pattern fieldPattern = Pattern.compile("\\$\\{(\\w+)\\}");
        Matcher matcher = fieldPattern.matcher(expr);

        while (matcher.find()) {
            String fieldName = matcher.group(1);
            Object fieldValue = data.get(fieldName);
            expr = expr.replace("${" + fieldName + "}",
                fieldValue instanceof String ? "\"" + fieldValue + "\"" : String.valueOf(fieldValue));
        }

        // 支持简单的三元表达式
        if (expr.contains("?") && expr.contains(":")) {
            return evaluateTernary(expr, data);
        }

        // 简单的字符串拼接
        if (expr.contains("||")) {
            return evaluateConcat(expr, data);
        }

        return expr;
    }

    /**
     * 计算三元表达式
     */
    private Object evaluateTernary(String expr, Map<String, Object> data) {
        // 简化实现，只支持基本格式: condition ? trueValue : falseValue
        String[] parts = expr.split("\\?");
        if (parts.length != 2) {
            return expr;
        }

        String condition = parts[0].trim();
        String[] values = parts[1].split(":");
        if (values.length != 2) {
            return expr;
        }

        boolean condResult = evaluateCondition(condition, data);
        return condResult ? values[0].trim() : values[1].trim();
    }

    /**
     * 计算条件
     */
    private boolean evaluateCondition(String condition, Map<String, Object> data) {
        condition = condition.trim();

        // 简单的相等判断
        if (condition.contains("==")) {
            String[] parts = condition.split("==");
            return parts[0].trim().equals(parts[1].trim().replace("\"", ""));
        }

        // 简单的不等判断
        if (condition.contains("!=")) {
            String[] parts = condition.split("!=");
            return !parts[0].trim().equals(parts[1].trim().replace("\"", ""));
        }

        return Boolean.parseBoolean(condition);
    }

    /**
     * 计算字符串拼接
     */
    private String evaluateConcat(String expr, Map<String, Object> data) {
        String[] parts = expr.split("\\|\\|");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
                sb.append(trimmed.substring(1, trimmed.length() - 1));
            } else {
                Object value = data.get(trimmed);
                sb.append(value != null ? value : "");
            }
        }
        return sb.toString();
    }

    /**
     * 字段拼接
     */
    private String applyFieldConcat(Map<String, Object> data, List<String> sourceFields, String separator) {
        if (sourceFields == null || sourceFields.isEmpty()) {
            return "";
        }

        String sep = separator != null ? separator : "";
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < sourceFields.size(); i++) {
            if (i > 0) {
                sb.append(sep);
            }
            Object value = data.get(sourceFields.get(i));
            sb.append(value != null ? value : "");
        }

        return sb.toString();
    }

    /**
     * 正则替换
     */
    private String applyRegexReplace(String value, String pattern, String replacement) {
        if (pattern == null || replacement == null) {
            return value;
        }

        try {
            return value.replaceAll(pattern, replacement);
        } catch (Exception e) {
            log.error("正则替换失败: pattern={}", pattern, e);
            return value;
        }
    }

    /**
     * 检查是否被过滤
     */
    private boolean isFiltered(Map<String, Object> data, List<TransformRule> rules) {
        for (TransformRule rule : rules) {
            if (!rule.isEnabled() || rule.getType() != TransformRule.TransformType.FILTER) {
                continue;
            }

            Object value = data.get(rule.getFieldName());
            Object filterValue = rule.getFilterValue();

            // 等于过滤
            if (rule.getFilterOperator() == TransformRule.FilterOperator.EQUALS) {
                if (Objects.equals(value, filterValue)) {
                    return true;
                }
            }

            // 不等于过滤
            if (rule.getFilterOperator() == TransformRule.FilterOperator.NOT_EQUALS) {
                if (!Objects.equals(value, filterValue)) {
                    return true;
                }
            }

            // 空值过滤
            if (rule.getFilterOperator() == TransformRule.FilterOperator.IS_NULL) {
                if (value == null) {
                    return true;
                }
            }

            // 非空过滤
            if (rule.getFilterOperator() == TransformRule.FilterOperator.IS_NOT_NULL) {
                if (value != null) {
                    return true;
                }
            }

            // 正则匹配过滤
            if (rule.getFilterOperator() == TransformRule.FilterOperator.REGEX_MATCH) {
                if (value != null && String.valueOf(value).matches(String.valueOf(filterValue))) {
                    return true;
                }
            }
        }

        return false;
    }
}
