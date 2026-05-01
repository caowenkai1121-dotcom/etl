package com.etl.engine.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建同步任务请求
 */
@Data
public class TaskCreateRequest {

    @NotBlank(message = "任务名称不能为空")
    private String name;

    private String description;

    @NotNull(message = "源数据源ID不能为空")
    private Long sourceDsId;

    @NotNull(message = "目标数据源ID不能为空")
    private Long targetDsId;

    @NotBlank(message = "同步模式不能为空")
    private String syncMode;

    @NotBlank(message = "同步范围不能为空")
    private String syncScope;

    @NotBlank(message = "表配置不能为空")
    private String tableConfig;

    private String fieldMapping;
    private String incrementalField;
    private String incrementalValue;
    private String cronExpression;
    private String syncStrategy = "OVERWRITE";
    private Integer batchSize = 1000;
    private Integer parallelThreads = 1;
    private Integer retryTimes = 3;
    private Integer retryInterval = 60;
}
