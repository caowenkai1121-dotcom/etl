package com.etl.common.domain;

import lombok.Data;

import java.io.Serializable;

/**
 * 字段信息
 */
@Data
public class ColumnInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 字段名
     */
    private String columnName;

    /**
     * 字段类型
     */
    private String columnType;

    /**
     * 字段长度
     */
    private Integer columnLength;

    /**
     * 小数位数
     */
    private Integer decimalDigits;

    /**
     * 是否可空
     */
    private Boolean nullable;

    /**
     * 默认值
     */
    private String defaultValue;

    /**
     * 字段注释
     */
    private String columnComment;

    /**
     * 是否主键
     */
    private Boolean primaryKey;

    /**
     * 是否自增
     */
    private Boolean autoIncrement;

    /**
     * 字段位置
     */
    private Integer ordinalPosition;
}
