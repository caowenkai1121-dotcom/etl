package com.etl.scheduler.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 调度信息响应
 */
@Data
public class ScheduleInfoResponse {

    private boolean scheduled;
    private String state;
    private String cronExpression;
    private LocalDateTime nextFireTime;
    private LocalDateTime previousFireTime;
}
