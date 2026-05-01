package com.etl.scheduler.dag;

import lombok.Data;

@Data
public class DagNode {
    private String code;
    private String name;
    private Long taskId;
    private String triggerCondition; // SUCCESS, FAILURE, TIMEOUT
    private Integer timeoutSeconds;
    private Integer x; // 画布位置
    private Integer y;
}
