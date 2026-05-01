package com.etl.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * CDC配置创建请求
 */
@Data
@Schema(description = "CDC配置创建请求")
public class CdcConfigRequest {

    @Schema(description = "配置名称", required = true)
    @NotBlank(message = "配置名称不能为空")
    private String name;

    @Schema(description = "关联数据源ID", required = true)
    @NotNull(message = "数据源ID不能为空")
    private Long datasourceId;

    @Schema(description = "连接器名称", required = true)
    @NotBlank(message = "连接器名称不能为空")
    private String connectorName;

    @Schema(description = "连接器类型(mysql/postgresql)")
    private String connectorType;

    @Schema(description = "服务器名称(Kafka Topic前缀)")
    private String serverName;

    @Schema(description = "数据库主机，为空则使用数据源配置")
    private String databaseHost;

    @Schema(description = "数据库端口，为空则使用数据源配置")
    private Integer databasePort;

    @Schema(description = "数据库用户名，为空则使用数据源配置")
    private String dbUsername;

    @Schema(description = "数据库密码，为空则使用数据源配置")
    private String dbPassword;

    @Schema(description = "表过滤配置，格式: db1.table1,db2.table2")
    private String filterRegex;

    @Schema(description = "黑名单过滤配置")
    private String filterBlackRegex;

    @Schema(description = "Kafka Topic前缀，为空则使用serverName")
    private String kafkaTopicPrefix;

    @Schema(description = "扩展配置(JSON格式)")
    private String extraConfig;

    @Schema(description = "状态: 0-禁用, 1-启用")
    private Integer status;

    @Schema(description = "备注")
    private String remark;
}