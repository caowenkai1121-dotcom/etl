package com.etl.api.controller;

import com.etl.common.result.Result;
import com.etl.engine.service.SystemConfigService;
import com.etl.monitor.cleanup.HistoryCleanupJob;
import com.etl.datasource.metadata.MetadataCacheManager;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 运维管理控制器
 */
@RestController
@RequestMapping("/maintenance")
@RequiredArgsConstructor
public class MaintenanceController {

    private final HistoryCleanupJob historyCleanupJob;
    private final MetadataCacheManager metadataCacheManager;
    private final SystemConfigService systemConfigService;

    @PostMapping("/clean-logs")
    public Result<Void> cleanLogs() {
        historyCleanupJob.cleanup();
        return Result.success();
    }

    @PostMapping("/clear-cache")
    public Result<Void> clearCache() {
        // 清除所有缓存（传入null会匹配所有前缀）
        metadataCacheManager.invalidateAll();
        systemConfigService.refresh();
        return Result.success();
    }
}
