package com.etl.engine.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 更新同步任务请求
 */
@Data
public class TaskUpdateRequest {

    @NotNull(message = "任务ID不能为空")
    private Long id;

    private String name;
    private String description;
    private Long sourceDsId;
    private Long targetDsId;
    private String syncMode;
    private String syncScope;
    private String tableConfig;
    private String fieldMapping;
    private String incrementalField;
    private String incrementalValue;
    private String cronExpression;
    private String syncStrategy;
    private Integer batchSize;
    private Integer parallelThreads;
    private Integer retryTimes;
    private Integer retryInterval;
}
