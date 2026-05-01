package com.etl.engine.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * API服务创建请求
 */
@Data
public class ApiServiceCreateRequest {

    /**
     * API名称
     */
    @NotBlank(message = "API名称不能为空")
    private String name;

    /**
     * API路径
     */
    @NotBlank(message = "API路径不能为空")
    private String path;

    /**
     * 请求方法
     */
    private String method = "GET";

    /**
     * 关联数据源ID
     */
    private Long datasourceId;

    /**
     * SQL模板
     */
    @NotBlank(message = "SQL模板不能为空")
    private String sqlTemplate;

    /**
     * 参数配置JSON
     */
    private String paramsConfig;

    /**
     * 认证方式
     */
    private String authType = "TOKEN";

    /**
     * 认证配置
     */
    private String authConfig;

    /**
     * 限流(次/分钟)
     */
    private Integer rateLimit = 100;

    /**
     * 超时时间(秒)
     */
    private Integer timeout = 30;

    /**
     * 描述
     */
    private String description;

    /**
     * 所属文件夹ID
     */
    private Long folderId;
}
