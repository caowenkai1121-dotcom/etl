package com.etl.api.controller;

import com.etl.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查Controller
 */
@Tag(name = "系统管理", description = "系统健康检查")
@RestController
@RequestMapping
public class HealthController {

    @Operation(summary = "健康检查")
    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        health.put("version", "1.0.0");
        return Result.success(health);
    }

    @Operation(summary = "系统信息")
    @GetMapping("/info")
    public Result<Map<String, Object>> info() {
        Map<String, Object> info = new HashMap<>();
        info.put("name", "ETL数据同步调度系统");
        info.put("version", "1.0.0");
        info.put("description", "企业级实时ETL数据同步调度系统");
        info.put("java", System.getProperty("java.version"));
        info.put("os", System.getProperty("os.name"));
        return Result.success(info);
    }
}
