package com.etl.api.controller;

import com.etl.common.result.Result;
import com.etl.datasource.connector.ConnectionPoolManager;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/health")
public class HealthDetailController {

    private final DataSource dataSource;

    public HealthDetailController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/detail")
    public Result<Map<String, Object>> detail() {
        Map<String, Object> health = new LinkedHashMap<>();

        // 1. 主数据库连接状态
        health.put("database", checkDatabase());

        // 2. 连接池状态摘要
        health.put("connectionPools", checkConnectionPools());

        // 3. JVM内存
        Runtime runtime = Runtime.getRuntime();
        Map<String, Object> jvm = new LinkedHashMap<>();
        jvm.put("maxMemory", formatBytes(runtime.maxMemory()));
        jvm.put("totalMemory", formatBytes(runtime.totalMemory()));
        jvm.put("freeMemory", formatBytes(runtime.freeMemory()));
        jvm.put("usedMemory", formatBytes(runtime.totalMemory() - runtime.freeMemory()));
        jvm.put("usedPercent", String.format("%.1f%%",
            (double)(runtime.totalMemory() - runtime.freeMemory()) / runtime.maxMemory() * 100));
        health.put("jvm", jvm);

        // 4. 磁盘空间
        Map<String, Object> disk = new LinkedHashMap<>();
        File file = new File("/");
        disk.put("total", formatBytes(file.getTotalSpace()));
        disk.put("free", formatBytes(file.getFreeSpace()));
        disk.put("usable", formatBytes(file.getUsableSpace()));
        health.put("disk", disk);

        return Result.success(health);
    }

    private Map<String, Object> checkDatabase() {
        Map<String, Object> db = new LinkedHashMap<>();
        try {
            // 通过 DataSource 测试连接
            dataSource.getConnection().close();
            db.put("status", "UP");
        } catch (Exception e) {
            db.put("status", "DOWN");
            db.put("error", e.getMessage());
        }
        return db;
    }

    private Map<String, Object> checkConnectionPools() {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            Map<String, Map<String, Object>> pools = ConnectionPoolManager.getAllPoolStatus();
            result.put("pools", pools);
            result.put("totalPools", pools.size());
        } catch (Exception e) {
            result.put("error", e.getMessage());
        }
        return result;
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
