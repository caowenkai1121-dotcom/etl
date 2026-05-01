package com.etl.common.domain;

import com.etl.common.enums.CdcEventType;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * CDC事件
 */
@Data
public class CdcEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 数据源ID
     */
    private Long datasourceId;

    /**
     * 数据库名
     */
    private String database;

    /**
     * 表名
     */
    private String table;

    /**
     * 事件类型
     */
    private CdcEventType eventType;

    /**
     * 变更前的数据
     */
    private Map<String, Object> beforeData;

    /**
     * 变更后的数据
     */
    private Map<String, Object> afterData;

    /**
     * 主键值
     */
    private Map<String, Object> primaryKeys;

    /**
     * MySQL类型映射 (列名 -> MySQL类型)
     */
    private Map<String, String> mysqlTypes;

    /**
     * 时间戳
     */
    private long timestamp;

    /**
     * 位点信息
     */
    private String position;

    /**
     * GTID(MySQL)
     */
    private String gtid;
}
