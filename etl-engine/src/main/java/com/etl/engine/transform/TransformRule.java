package com.etl.engine.transform;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 转换规则
 */
@Data
public class TransformRule {

    private Long id;
    private String name;
    private String description;
    private boolean enabled = true;
    private int sortOrder = 0;

    /**
     * 规则类型
     */
    private TransformType type;

    /**
     * 源字段名
     */
    private String fieldName;

    /**
     * 目标字段名（用于重命名、添加字段等）
     */
    private String targetField;

    /**
     * 值映射表
     */
    private Map<String, Object> valueMapping;

    /**
     * 格式模式
     */
    private String formatPattern;

    /**
     * 表达式
     */
    private String expression;

    /**
     * 源字段列表（用于拼接）
     */
    private List<String> sourceFields;

    /**
     * 分隔符
     */
    private String separator;

    /**
     * 默认值
     */
    private Object defaultValue;

    /**
     * 正则模式
     */
    private String regexPattern;

    /**
     * 替换值
     */
    private String replacement;

    /**
     * 过滤操作符
     */
    private FilterOperator filterOperator;

    /**
     * 过滤值
     */
    private Object filterValue;

    /**
     * 遇错是否停止
     */
    private boolean stopOnError = false;

    /**
     * 转换类型枚举
     */
    public enum TransformType {
        VALUE_MAP("值映射"),
        FORMAT_CONVERT("格式转换"),
        EXPRESSION("表达式计算"),
        FIELD_CONCAT("字段拼接"),
        DEFAULT_VALUE("默认值"),
        TRIM("去除空白"),
        UPPER_CASE("转大写"),
        LOWER_CASE("转小写"),
        REGEX_REPLACE("正则替换"),
        FIELD_RENAME("字段重命名"),
        FIELD_ADD("添加字段"),
        FIELD_REMOVE("移除字段"),
        FILTER("数据过滤");

        private final String description;

        TransformType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 过滤操作符枚举
     */
    public enum FilterOperator {
        EQUALS("等于"),
        NOT_EQUALS("不等于"),
        IS_NULL("为空"),
        IS_NOT_NULL("不为空"),
        REGEX_MATCH("正则匹配");

        private final String description;

        FilterOperator(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
