package com.etl.datasource.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 更新数据源请求
 */
@Data
public class DatasourceUpdateRequest {

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

    /** 为空表示不修改密码 */
    private String password;

    private String charset;

    private String extraConfig;

    private String remark;
}
