package com.etl.api.controller;

import com.etl.common.result.Result;
import com.etl.monitor.service.CdcMonitorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * CDC监控Controller
 */
@Tag(name = "CDC监控", description = "CDC任务健康检查和监控告警")
@RestController
@RequestMapping("/cdc/monitor")
@RequiredArgsConstructor
public class CdcMonitorController {

    private final CdcMonitorService cdcMonitorService;

    @Operation(summary = "获取CDC健康报告")
    @GetMapping("/health")
    public Result<Map<String, Object>> getHealthReport() {
        return Result.success(cdcMonitorService.getHealthReport());
    }
}
