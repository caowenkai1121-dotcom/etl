package com.etl.monitor.service;

import com.etl.engine.cdc.CdcManagerService;
import com.etl.engine.mapper.CdcPositionMapper;
import com.etl.engine.mapper.SyncLogMapper;
import com.etl.engine.mapper.TaskExecutionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * CDC监控服务
 * 提供实时监控、健康检查和异常告警功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CdcMonitorService {

    private final CdcManagerService cdcManagerService;
    private final TaskExecutionMapper taskExecutionMapper;
    private final CdcPositionMapper cdcPositionMapper;
    private final SyncLogMapper syncLogMapper;

    // 告警阈值配置
    private static final long MAX_SILENCE_TIME_MS = 60000; // 最大静默时间60秒
    private static final int MAX_ERROR_COUNT = 5; // 最大错误次数
    private static final long MAX_LAG_MS = 30000; // 最大延迟30秒

    // 告警状态缓存 - 避免重复告警
    private final Map<Long, Long> lastAlertTime = new HashMap<>();
    private static final long ALERT_INTERVAL_MS = 300000; // 告警间隔5分钟

    /**
     * CDC健康检查 - 每30秒执行一次
     */
    @Scheduled(fixedRate = 30000)
    public void healthCheck() {
        try {
            var runningTasks = cdcManagerService.getAllRunningTasks();

            for (var entry : runningTasks.entrySet()) {
                Long taskId = entry.getKey();
                var task = entry.getValue();

                // 检查任务健康状态
                CdcHealthStatus status = checkTaskHealth(taskId, task);

                if (!status.isHealthy()) {
                    handleUnhealthyTask(taskId, status);
                }
            }

            log.debug("CDC健康检查完成: runningTasks={}", runningTasks.size());

        } catch (Exception e) {
            log.error("CDC健康检查异常", e);
        }
    }

    /**
     * 检查任务健康状态
     */
    private CdcHealthStatus checkTaskHealth(Long taskId, CdcManagerService.CdcRunningTask task) {
        CdcHealthStatus status = new CdcHealthStatus();
        status.setTaskId(taskId);
        status.setRunning(true);
        status.setProcessedCount(task.getProcessedCount());
        status.setDuration(task.getDuration());

        // 检查是否静默过久（没有处理任何事件）
        long silenceTime = System.currentTimeMillis() - task.getStartTime();
        if (task.getProcessedCount() == 0 && silenceTime > MAX_SILENCE_TIME_MS) {
            status.setHealthy(false);
            status.addIssue("TASK_SILENCE", "任务静默超过" + (MAX_SILENCE_TIME_MS / 1000) + "秒，未处理任何事件");
        }

        // 检查Kafka消费者状态
        if (task.getEngine() != null) {
            try {
                var stats = task.getEngine().getStatistics();
                long failed = stats.getOrDefault("failed", 0L);
                if (failed > MAX_ERROR_COUNT) {
                    status.setHealthy(false);
                    status.addIssue("HIGH_ERROR_RATE", "错误次数过高: " + failed);
                }
            } catch (Exception e) {
                status.setHealthy(false);
                status.addIssue("ENGINE_ERROR", "引擎状态异常: " + e.getMessage());
            }
        }

        return status;
    }

    /**
     * 处理不健康任务
     */
    private void handleUnhealthyTask(Long taskId, CdcHealthStatus status) {
        // 检查是否在告警间隔内
        Long lastAlert = lastAlertTime.get(taskId);
        if (lastAlert != null && System.currentTimeMillis() - lastAlert < ALERT_INTERVAL_MS) {
            log.debug("告警间隔内，跳过: taskId={}", taskId);
            return;
        }

        // 记录告警
        lastAlertTime.put(taskId, System.currentTimeMillis());

        // 记录告警日志
        log.warn("CDC任务健康告警: taskId={}, issues={}", taskId, status.getIssues());

        // 发送告警（可扩展为邮件、钉钉、企业微信等）
        sendAlert(taskId, status);
    }

    /**
     * 发送告警通知
     */
    private void sendAlert(Long taskId, CdcHealthStatus status) {
        // 构建告警消息
        StringBuilder message = new StringBuilder();
        message.append("【CDC任务告警】\n");
        message.append("任务ID: ").append(taskId).append("\n");
        message.append("运行时长: ").append(formatDuration(status.getDuration())).append("\n");
        message.append("处理事件数: ").append(status.getProcessedCount()).append("\n");
        message.append("问题列表:\n");
        for (var issue : status.getIssues().entrySet()) {
            message.append("  - ").append(issue.getKey()).append(": ").append(issue.getValue()).append("\n");
        }
        message.append("时间: ").append(LocalDateTime.now()).append("\n");

        // 记录告警日志
        log.warn("告警消息:\n{}", message);

        // TODO: 可扩展以下告警渠道
        // 1. 发送邮件
        // 2. 发送钉钉消息
        // 3. 发送企业微信消息
        // 4. 调用Webhook
    }

    /**
     * 获取CDC任务健康报告
     */
    public Map<String, Object> getHealthReport() {
        Map<String, Object> report = new LinkedHashMap<>();

        // 运行中的任务
        var runningTasks = cdcManagerService.getAllRunningTasks();
        List<Map<String, Object>> taskList = new ArrayList<>();

        for (var entry : runningTasks.entrySet()) {
            Long taskId = entry.getKey();
            var task = entry.getValue();

            Map<String, Object> taskInfo = new LinkedHashMap<>();
            taskInfo.put("taskId", taskId);
            taskInfo.put("running", task.isRunning());
            taskInfo.put("duration", formatDuration(task.getDuration()));
            taskInfo.put("processedCount", task.getProcessedCount());
            taskInfo.put("startTime", new Date(task.getStartTime()));
            taskInfo.put("sourceDbType", task.getSourceDbType());

            // 获取引擎统计
            if (task.getEngine() != null) {
                try {
                    taskInfo.put("statistics", task.getEngine().getStatistics());
                } catch (Exception e) {
                    taskInfo.put("statisticsError", e.getMessage());
                }
            }

            // 健康检查
            CdcHealthStatus health = checkTaskHealth(taskId, task);
            taskInfo.put("healthy", health.isHealthy());
            if (!health.isHealthy()) {
                taskInfo.put("issues", health.getIssues());
            }

            taskList.add(taskInfo);
        }

        report.put("totalRunning", runningTasks.size());
        report.put("tasks", taskList);
        report.put("checkTime", LocalDateTime.now());

        // 系统状态
        Map<String, Object> systemStatus = new LinkedHashMap<>();
        systemStatus.put("maxSilenceTimeMs", MAX_SILENCE_TIME_MS);
        systemStatus.put("maxErrorCount", MAX_ERROR_COUNT);
        systemStatus.put("maxLagMs", MAX_LAG_MS);
        systemStatus.put("alertIntervalMs", ALERT_INTERVAL_MS);
        report.put("config", systemStatus);

        return report;
    }

    /**
     * 格式化时长
     */
    private String formatDuration(long ms) {
        long seconds = ms / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        if (hours > 0) {
            return String.format("%d小时%d分钟", hours, minutes % 60);
        } else if (minutes > 0) {
            return String.format("%d分钟%d秒", minutes, seconds % 60);
        } else {
            return String.format("%d秒", seconds);
        }
    }

    /**
     * CDC健康状态
     */
    @lombok.Data
    public static class CdcHealthStatus {
        private Long taskId;
        private boolean healthy = true;
        private boolean running;
        private long processedCount;
        private long duration;
        private Map<String, String> issues = new LinkedHashMap<>();

        public void addIssue(String code, String message) {
            issues.put(code, message);
            healthy = false;
        }
    }
}
