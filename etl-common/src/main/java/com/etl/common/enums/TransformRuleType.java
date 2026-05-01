package com.etl.common.enums;

import lombok.Getter;

@Getter
public enum TransformRuleType {
    FIELD_MAP("字段映射"),
    FIELD_RENAME("字段重命名"),
    FIELD_REMOVE("字段移除"),
    FIELD_ADD("字段添加"),
    CONSTANT_ADD("常量添加"),
    VALUE_MAP("值映射"),
    FORMAT_CONVERT("格式转换"),
    EXPRESSION("表达式计算"),
    CASE_CONVERT("大小写转换"),
    TRIM("去空白"),
    REGEX_REPLACE("正则替换"),
    NOT_NULL("非空处理"),
    DEFAULT_VALUE("默认值"),
    CONDITION_FILTER("条件过滤"),
    ENCRYPT("加密解密"),
    DESENSITIZE("数据脱敏"),
    JSON_PARSE("JSON解析"),
    JSON_EXTRACT("JSON提取"),
    JSON_FLATTEN("JSON展开"),
    AGGREGATE("聚合计算"),
    JOIN("多源JOIN"),
    UNION("多源UNION"),
    LOOKUP("查找表"),
    CONDITION_BRANCH("条件分支"),
    DATE_FORMAT("日期格式转换"),
    DATE_CALC("日期计算"),
    TYPE_INFER("类型推断");

    private final String description;

    TransformRuleType(String description) {
        this.description = description;
    }
}
