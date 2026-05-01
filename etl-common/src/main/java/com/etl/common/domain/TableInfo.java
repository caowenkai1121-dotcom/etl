package com.etl.common.domain;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 表信息
 */
@Data
public class TableInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 表名
     */
    private String tableName;

    /**
     * 表注释
     */
    private String tableComment;

    /**
     * 表类型(TABLE/VIEW)
     */
    private String tableType;

    /**
     * Schema名称
     */
    private String schemaName;

    /**
     * 字段列表
     */
    private List<ColumnInfo> columns;

    /**
     * 主键列名列表
     */
    private List<String> primaryKeys;

    /**
     * 行数估算
     */
    private Long rowCount;
}
