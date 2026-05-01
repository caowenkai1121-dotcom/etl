package com.etl.datasource.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 数据源响应（隐藏密码）
 */
@Data
public class DatasourceResponse {

    private Long id;
    private String name;
    private String type;
    private String host;
    private Integer port;
    private String databaseName;
    private String username;
    private String charset;
    private String extraConfig;
    private Integer status;
    private Integer connectionTest;
    private LocalDateTime lastTestTime;
    private String remark;
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;

    // ========== CDC配置状态 ==========

    /**
     * CDC配置ID（如果存在）
     */
    private Long cdcConfigId;

    /**
     * CDC同步状态：STOPPED/RUNNING/ERROR/null(未配置)
     */
    private String cdcSyncStatus;

    /**
     * CDC是否启用
     */
    private Boolean cdcEnabled;

    public static DatasourceResponse from(com.etl.datasource.entity.EtlDatasource entity) {
        if (entity == null) return null;
        DatasourceResponse resp = new DatasourceResponse();
        resp.setId(entity.getId());
        resp.setName(entity.getName());
        resp.setType(entity.getType());
        resp.setHost(entity.getHost());
        resp.setPort(entity.getPort());
        resp.setDatabaseName(entity.getDatabaseName());
        resp.setUsername(entity.getUsername());
        resp.setCharset(entity.getCharset());
        resp.setExtraConfig(entity.getExtraConfig());
        resp.setStatus(entity.getStatus());
        resp.setConnectionTest(entity.getConnectionTest());
        resp.setLastTestTime(entity.getLastTestTime());
        resp.setRemark(entity.getRemark());
        resp.setCreatedBy(entity.getCreatedBy());
        resp.setCreatedAt(entity.getCreatedAt());
        resp.setUpdatedBy(entity.getUpdatedBy());
        resp.setUpdatedAt(entity.getUpdatedAt());
        return resp;
    }
}
