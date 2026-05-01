package com.etl.engine.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * API服务响应DTO
 */
@Data
public class ApiServiceResponse {

    private Long id;

    private String name;

    private String path;

    private String method;

    private Long datasourceId;

    private String datasourceName;

    private String sqlTemplate;

    private String paramsConfig;

    private String authType;

    private String authConfig;

    private Integer rateLimit;

    private Integer timeout;

    private String status;

    private String description;

    private Long folderId;

    private String folderName;

    private String createBy;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /**
     * 今日调用次数
     */
    private Integer todayCalls;

    /**
     * 平均响应时间
     */
    private Integer avgResponseTime;
}
