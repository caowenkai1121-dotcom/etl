package com.etl.datasource.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建数据源请求
 */
@Data
public class DatasourceCreateRequest {

    @NotBlank(message = "数据源名称不能为空")
    private String name;

    @NotBlank(message = "数据源类型不能为空")
    private String type;

    @NotBlank(message = "主机地址不能为空")
    private String host;

    @NotNull(message = "端口号不能为空")
    private Integer port;

    @NotBlank(message = "数据库名称不能为空")
    private String databaseName;

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    private String charset = "utf8mb4";

    private String extraConfig;

    private String remark;

    // ========== CDC配置选项（MySQL/PostgreSQL支持） ==========

    /**
     * 是否启用CDC同步（MySQL/PostgreSQL支持）
     * 默认true：创建数据源时自动启用CDC
     */
    private Boolean enableCdc = true;

    /**
     * CDC表过滤配置，格式：db1.table1,db2.table2
     * 默认：同步该数据源所有表 {databaseName}..*
     */
    private String cdcFilterRegex;

    /**
     * CDC黑名单过滤配置
     */
    private String cdcFilterBlackRegex;

    /**
     * 是否立即部署连接器（enableCdc=true时生效）
     * 默认true：创建配置后立即部署到Debezium Connect
     */
    private Boolean deployConnector = true;
}
