package com.etl.datasource.connector;

import lombok.Data;

import java.util.Map;

/**
 * CDC 配置
 */
@Data
public class CdcConfig {

    /**
     * CDC 类型: CANAL, DEBEZIUM, NATIVE
     */
    private String cdcType;

    /**
     * 服务器地址
     */
    private String serverHost;

    /**
     * 服务器端口
     */
    private Integer serverPort;

    /**
     * 数据库实例
     */
    private String database;

    /**
     * 要监听的表
     */
    private String[] tables;

    /**
     * 起始位置
     */
    private String startPosition;

    /**
     * 是否包含 DDL
     */
    private boolean includeDdl = false;

    /**
     * 扩展配置
     */
    private Map<String, Object> extraConfig;
}
