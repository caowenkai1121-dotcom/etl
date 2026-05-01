package com.etl.common.domain;

import lombok.Data;

import java.io.Serializable;

/**
 * 字段映射
 */
@Data
public class FieldMapping implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 源字段名
     */
    private String sourceField;

    /**
     * 源字段类型
     */
    private String sourceType;

    /**
     * 目标字段名
     */
    private String targetField;

    /**
     * 目标字段类型
     */
    private String targetType;

    /**
     * 转换表达式
     */
    private String transformExpression;

    /**
     * 是否主键
     */
    private Boolean primaryKey;
}
