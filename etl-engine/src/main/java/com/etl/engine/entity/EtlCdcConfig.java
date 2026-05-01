package com.etl.engine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * CDC配置实体
 * 用于存储Debezium连接器配置，支持多数据源CDC管理
 */
@Data
@TableName("etl_cdc_config")
public class EtlCdcConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 配置名称
     */
    private String name;

    /**
     * 关联数据源ID
     */
    private Long datasourceId;

    /**
     * 连接器名称
     * 全局唯一，用于标识不同的CDC通道
     * 格式建议: etl-{dbType}-{datasourceId}
     */
    private String connectorName;

    /**
     * 连接器类型
     * mysql / postgresql
     */
    private String connectorType;

    /**
     * 服务器名称
     * 用作Kafka Topic前缀
     * 默认: etl-{connectorType}-{id}
     */
    private String serverName;

    /**
     * 数据库主机地址
     * 为空则使用数据源配置
     */
    private String databaseHost;

    /**
     * 数据库端口
     * 为空则使用数据源配置
     */
    private Integer databasePort;

    /**
     * 数据库用户名
     * 为空则使用数据源配置
     */
    private String dbUsername;

    /**
     * 数据库密码(加密存储)
     * 为空则使用数据源配置
     */
    private String dbPassword;

    /**
     * 表过滤配置
     * 格式: db1.table1,db2.table2
     * 默认: 所有库所有表
     */
    private String filterRegex;

    /**
     * 黑名单过滤配置
     * 格式: db1.table1,db2.table2
     */
    private String filterBlackRegex;

    /**
     * Kafka Topic前缀
     * 实际Topic格式: {topicPrefix}.{database}.{table}
     * 为空则使用serverName
     */
    private String kafkaTopicPrefix;

    /**
     * 扩展配置(JSON格式)
     * 用于存储Debezium连接器的高级配置
     * 如: snapshot.mode, snapshot.locking.mode等
     */
    private String extraConfig;

    /**
     * 状态: 0-禁用, 1-启用
     */
    private Integer status;

    /**
     * 同步状态: STOPPED/RUNNING/ERROR/PAUSED
     */
    private String syncStatus;

    /**
     * 最后同步时间
     */
    private LocalDateTime lastSyncTime;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 创建人
     */
    private String createdBy;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新人
     */
    private String updatedBy;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 逻辑删除: 0-未删除, 1-已删除
     */
    @TableLogic
    private Integer deleted;
}
