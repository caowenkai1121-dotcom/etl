package com.etl.monitor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 告警规则请求
 */
@Data
public class AlertRuleRequest {

    @NotBlank(message = "规则名称不能为空")
    private String name;

    @NotBlank(message = "告警类型不能为空")
    private String alertType;

    private String description;

    @NotBlank(message = "触发条件不能为空")
    private String conditionExpr;

    @NotBlank(message = "告警级别不能为空")
    private String severity;

    @NotNull(message = "通知渠道不能为空")
    private String channels;

    private String recipients;

    private Integer enabled = 1;

    private Integer silenceMinutes = 30;
}
