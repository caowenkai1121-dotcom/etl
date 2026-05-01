package com.etl.api.dto;

import com.etl.engine.entity.EtlCdcConfig;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * CDC配置响应
 */
@Data
@Schema(description = "CDC配置响应")
public class CdcConfigResponse {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "配置名称")
    private String name;

    @Schema(description = "关联数据源ID")
    private Long datasourceId;

    @Schema(description = "数据源名称")
    private String datasourceName;

    @Schema(description = "连接器名称")
    private String connectorName;

    @Schema(description = "连接器类型(mysql/postgresql)")
    private String connectorType;

    @Schema(description = "服务器名称(Kafka Topic前缀)")
    private String serverName;

    @Schema(description = "数据库主机")
    private String databaseHost;

    @Schema(description = "数据库端口")
    private Integer databasePort;

    @Schema(description = "数据库用户名")
    private String dbUsername;

    @Schema(description = "表过滤配置")
    private String filterRegex;

    @Schema(description = "黑名单过滤配置")
    private String filterBlackRegex;

    @Schema(description = "Kafka Topic前缀")
    private String kafkaTopicPrefix;

    @Schema(description = "扩展配置")
    private String extraConfig;

    @Schema(description = "状态: 0-禁用, 1-启用")
    private Integer status;

    @Schema(description = "同步状态: STOPPED/RUNNING/ERROR/PAUSED")
    private String syncStatus;

    @Schema(description = "最后同步时间")
    private LocalDateTime lastSyncTime;

    @Schema(description = "错误信息")
    private String errorMessage;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    /**
     * 从实体转换
     */
    public static CdcConfigResponse from(EtlCdcConfig entity) {
        if (entity == null) {
            return null;
        }
        CdcConfigResponse response = new CdcConfigResponse();
        response.setId(entity.getId());
        response.setName(entity.getName());
        response.setDatasourceId(entity.getDatasourceId());
        response.setConnectorName(entity.getConnectorName());
        response.setConnectorType(entity.getConnectorType());
        response.setServerName(entity.getServerName());
        response.setDatabaseHost(entity.getDatabaseHost());
        response.setDatabasePort(entity.getDatabasePort());
        response.setDbUsername(entity.getDbUsername());
        response.setFilterRegex(entity.getFilterRegex());
        response.setFilterBlackRegex(entity.getFilterBlackRegex());
        response.setKafkaTopicPrefix(entity.getKafkaTopicPrefix());
        response.setExtraConfig(entity.getExtraConfig());
        response.setStatus(entity.getStatus());
        response.setSyncStatus(entity.getSyncStatus());
        response.setLastSyncTime(entity.getLastSyncTime());
        response.setErrorMessage(entity.getErrorMessage());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }

    /**
     * 从实体转换（含数据源名称）
     */
    public static CdcConfigResponse from(EtlCdcConfig entity, String datasourceName) {
        CdcConfigResponse response = from(entity);
        if (response != null) {
            response.setDatasourceName(datasourceName);
        }
        return response;
    }
}