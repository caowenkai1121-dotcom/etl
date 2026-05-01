package com.etl.api.controller;

import com.etl.common.result.PageResult;
import com.etl.common.result.Result;
import com.etl.engine.entity.EtlSystemConfig;
import com.etl.engine.service.SystemConfigService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 系统配置控制器
 */
@RestController
@RequestMapping("/config")
@RequiredArgsConstructor
public class SystemConfigController {

    private final SystemConfigService systemConfigService;

    @GetMapping("/list")
    public Result<?> listConfig(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String group) {
        PageResult<EtlSystemConfig> result = systemConfigService.pageList(page, size, group);
        return Result.success(result);
    }

    @GetMapping("/{group}/{key}")
    public Result<?> getConfig(@PathVariable String group, @PathVariable String key) {
        String value = systemConfigService.getString(group, key, null);
        if (value == null) {
            return Result.error(404, "配置不存在");
        }
        return Result.success(value);
    }

    @PostMapping
    public Result<Void> createConfig(@RequestBody ConfigCreateRequest request) {
        try {
            systemConfigService.updateConfig(request.getGroup(), request.getKey(),
                    request.getValue(), request.getDescription());
            return Result.success();
        } catch (Exception e) {
            return Result.error(400, e.getMessage());
        }
    }

    @PutMapping("/{group}/{key}")
    public Result<Void> updateConfig(@PathVariable String group, @PathVariable String key,
                                     @RequestBody ConfigUpdateRequest request) {
        try {
            systemConfigService.updateConfig(group, key, request.getValue(), request.getDescription());
            return Result.success();
        } catch (Exception e) {
            return Result.error(400, e.getMessage());
        }
    }

    @DeleteMapping("/{group}/{key}")
    public Result<Void> deleteConfig(@PathVariable String group, @PathVariable String key) {
        systemConfigService.deleteConfig(group, key);
        return Result.success();
    }

    @Data
    public static class ConfigCreateRequest {
        private String group;
        private String key;
        private String value;
        private String description;
    }

    @Data
    public static class ConfigUpdateRequest {
        private String value;
        private String description;
    }
}
