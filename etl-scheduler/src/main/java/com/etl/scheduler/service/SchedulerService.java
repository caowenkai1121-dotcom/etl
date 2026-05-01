package com.etl.scheduler.service;

import com.alibaba.fastjson2.JSON;
import com.etl.common.enums.ExecutionStatus;
import com.etl.common.enums.TaskStatus;
import com.etl.common.exception.EtlException;
import com.etl.engine.entity.EtlSyncTask;
import com.etl.engine.entity.EtlTaskDependency;
import com.etl.engine.entity.EtlTaskExecution;
import com.etl.engine.service.SyncTaskService;
import com.etl.engine.service.TaskExecutionService;
import com.etl.scheduler.dependency.TaskDependencyManager;
import com.etl.scheduler.dto.ScheduleInfoResponse;
import com.etl.scheduler.job.SyncJob;
import com.etl.scheduler.retry.TaskRetryExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * 调度服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulerService {

    private final Scheduler scheduler;
    private final SyncTaskService syncTaskService;
    private final TaskDependencyManager taskDependencyManager;
    private final TaskRetryExecutor taskRetryExecutor;
    private final TaskExecutionService taskExecutionService;

    private static final String JOB_GROUP = "ETL_SYNC_JOB_GROUP";
    private static final String TRIGGER_GROUP = "ETL_SYNC_TRIGGER_GROUP";

    /**
     * 初始化所有启用的定时任务
     */
    @Transactional(rollbackFor = Exception.class)
    public void initScheduledTasks() {
        List<EtlSyncTask> tasks = syncTaskService.getEnabledTasks();
        for (EtlSyncTask task : tasks) {
            if (task.getCronExpression() != null && !task.getCronExpression().isEmpty()) {
                try {
                    scheduleTask(task.getId(), task.getCronExpression());
                } catch (Exception e) {
                    log.error("初始化定时任务失败: taskId={}", task.getId(), e);
                }
            }
        }
        log.info("初始化定时任务完成，共{}个", tasks.size());
    }

    /**
     * 创建定时任务
     */
    @Transactional(rollbackFor = Exception.class)
    public void scheduleTask(Long taskId, String cronExpression) throws SchedulerException {
        EtlSyncTask task = syncTaskService.getDetail(taskId);
        if (task == null) {
            throw EtlException.taskNotFound(taskId);
        }

        // 验证Cron表达式
        if (!isValidCron(cronExpression)) {
            throw EtlException.configError("无效的Cron表达式: " + cronExpression);
        }

        // 先删除已存在的任务
        unscheduleTask(taskId);

        // 创建JobDetail
        JobDetail jobDetail = JobBuilder.newJob(SyncJob.class)
            .withIdentity("JOB_" + taskId, JOB_GROUP)
            .usingJobData("taskId", taskId)
            .storeDurably()
            .build();

        // 创建Trigger
        CronTrigger trigger = TriggerBuilder.newTrigger()
            .withIdentity("TRIGGER_" + taskId, TRIGGER_GROUP)
            .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression)
                .withMisfireHandlingInstructionFireAndProceed())
            .forJob(jobDetail)
            .build();

        // 调度任务
        scheduler.scheduleJob(jobDetail, trigger);

        // 更新任务状态
        syncTaskService.updateStatus(taskId, TaskStatus.RUNNING.getCode());

        // 计算下次执行时间
        Date nextFireTime = trigger.getNextFireTime();
        if (nextFireTime != null) {
            LocalDateTime nextSyncTime = LocalDateTime.ofInstant(
                nextFireTime.toInstant(), ZoneId.systemDefault());
            syncTaskService.updateSyncTime(taskId, nextSyncTime);
        }

        log.info("创建定时任务成功: taskId={}, cron={}", taskId, cronExpression);
    }

    /**
     * 删除定时任务
     */
    @Transactional(rollbackFor = Exception.class)
    public void unscheduleTask(Long taskId) throws SchedulerException {
        JobKey jobKey = getJobKey(taskId);
        TriggerKey triggerKey = getTriggerKey(taskId);

        if (scheduler.checkExists(triggerKey)) {
            scheduler.unscheduleJob(triggerKey);
        }

        if (scheduler.checkExists(jobKey)) {
            scheduler.deleteJob(jobKey);
        }

        // 更新任务状态
        syncTaskService.updateStatus(taskId, TaskStatus.STOPPED.getCode());

        log.info("删除定时任务成功: taskId={}", taskId);
    }

    /**
     * 暂停定时任务
     */
    @Transactional(rollbackFor = Exception.class)
    public void pauseTask(Long taskId) throws SchedulerException {
        JobKey jobKey = getJobKey(taskId);
        if (scheduler.checkExists(jobKey)) {
            scheduler.pauseJob(jobKey);
        }
        syncTaskService.updateStatus(taskId, TaskStatus.PAUSED.getCode());
        log.info("暂停定时任务成功: taskId={}", taskId);
    }

    /**
     * 恢复定时任务
     */
    @Transactional(rollbackFor = Exception.class)
    public void resumeTask(Long taskId) throws SchedulerException {
        JobKey jobKey = getJobKey(taskId);
        if (scheduler.checkExists(jobKey)) {
            scheduler.resumeJob(jobKey);
        }
        syncTaskService.updateStatus(taskId, TaskStatus.RUNNING.getCode());
        log.info("恢复定时任务成功: taskId={}", taskId);
    }

    /**
     * 立即执行一次任务
     */
    @Transactional(rollbackFor = Exception.class)
    public void triggerTask(Long taskId) throws SchedulerException {
        if (!checkDependencies(taskId)) {
            log.warn("任务依赖未满足，跳过执行: taskId={}", taskId);
            EtlTaskExecution execution = taskExecutionService.createExecution(taskId,
                ExecutionStatus.SKIPPED.getCode());
            taskExecutionService.completeExecution(execution.getId(), ExecutionStatus.SKIPPED.getCode(),
                "依赖任务未完成或失败", null);
            return;
        }

        JobKey jobKey = getJobKey(taskId);

        if (!scheduler.checkExists(jobKey)) {
            EtlSyncTask task = syncTaskService.getDetail(taskId);
            if (task != null && task.getCronExpression() != null) {
                scheduleTask(taskId, task.getCronExpression());
            }
        }

        scheduler.triggerJob(jobKey);
        log.info("触发任务执行: taskId={}", taskId);
    }

    /**
     * 检查任务依赖是否满足（批量查询优化，避免N+1）
     */
    private boolean checkDependencies(Long taskId) {
        List<EtlTaskDependency> dependencies = taskDependencyManager.getDependencies(taskId);
        if (dependencies.isEmpty()) return true;

        // 批量收集所有依赖任务ID
        List<Long> dependsOnIds = new ArrayList<>();
        for (EtlTaskDependency dep : dependencies) {
            dependsOnIds.add(dep.getDependsOnTaskId());
        }
        // 一次查询获取所有依赖任务的最新执行记录
        Map<Long, EtlTaskExecution> latestExecMap = taskExecutionService.getLatestExecutionsBatch(dependsOnIds);

        for (EtlTaskDependency dep : dependencies) {
            EtlTaskExecution latestExecution = latestExecMap.get(dep.getDependsOnTaskId());
            if (latestExecution == null) {
                log.debug("依赖任务从未执行: taskId={}, dependsOn={}", taskId, dep.getDependsOnTaskId());
                return false;
            }
            String status = latestExecution.getStatus();
            if (ExecutionStatus.SUCCESS.getCode().equals(dep.getDependencyType())) {
                if (!ExecutionStatus.SUCCESS.getCode().equals(status)) {
                    log.debug("依赖任务未成功: taskId={}, dependsOn={}, status={}",
                        taskId, dep.getDependsOnTaskId(), status);
                    return false;
                }
            } else {
                if (!ExecutionStatus.SUCCESS.getCode().equals(status) &&
                    !ExecutionStatus.FAILED.getCode().equals(status) &&
                    !ExecutionStatus.CANCELLED.getCode().equals(status)) {
                    log.debug("依赖任务未完成: taskId={}, dependsOn={}, status={}",
                        taskId, dep.getDependsOnTaskId(), status);
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 获取任务依赖列表
     */
    public List<EtlTaskDependency> getTaskDependencies(Long taskId) {
        return taskDependencyManager.getDependencies(taskId);
    }

    /**
     * 添加任务依赖
     */
    @Transactional(rollbackFor = Exception.class)
    public void addTaskDependency(Long taskId, Long dependsOnTaskId, String type) {
        taskDependencyManager.addDependency(taskId, dependsOnTaskId, type);
    }

    /**
     * 移除任务依赖
     */
    @Transactional(rollbackFor = Exception.class)
    public void removeTaskDependency(Long taskId, Long dependsOnTaskId) {
        taskDependencyManager.removeDependency(taskId, dependsOnTaskId);
    }

    /**
     * 更新任务的Cron表达式
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateCronExpression(Long taskId, String cronExpression) throws SchedulerException {
        EtlSyncTask task = syncTaskService.getDetail(taskId);
        if (task == null) {
            throw EtlException.taskNotFound(taskId);
        }

        if (!isValidCron(cronExpression)) {
            throw EtlException.configError("无效的Cron表达式: " + cronExpression);
        }

        TriggerKey triggerKey = getTriggerKey(taskId);
        JobKey jobKey = getJobKey(taskId);

        if (scheduler.checkExists(triggerKey)) {
            CronTrigger newTrigger = TriggerBuilder.newTrigger()
                .withIdentity(triggerKey)
                .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression)
                    .withMisfireHandlingInstructionFireAndProceed())
                .build();
            scheduler.rescheduleJob(triggerKey, newTrigger);
        } else if (scheduler.checkExists(jobKey)) {
            // trigger不存在但job存在，创建新trigger关联已有job
            CronTrigger newTrigger = TriggerBuilder.newTrigger()
                .withIdentity(triggerKey)
                .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression)
                    .withMisfireHandlingInstructionFireAndProceed())
                .forJob(jobKey)
                .build();
            scheduler.scheduleJob(newTrigger);
        }
        // trigger和job都不存在时仅更新数据库（下次调度会全量重建）

        task.setCronExpression(cronExpression);
        syncTaskService.updateById(task);

        log.info("更新任务Cron表达式: taskId={}, cron={}", taskId, cronExpression);
    }

    /**
     * 获取任务状态
     */
    public ScheduleInfoResponse getTaskScheduleInfo(Long taskId) throws SchedulerException {
        ScheduleInfoResponse info = new ScheduleInfoResponse();

        JobKey jobKey = getJobKey(taskId);
        TriggerKey triggerKey = getTriggerKey(taskId);

        if (scheduler.checkExists(jobKey)) {
            info.setScheduled(true);
            Trigger.TriggerState state = scheduler.getTriggerState(triggerKey);
            info.setState(state.name());

            CronTrigger trigger = (CronTrigger) scheduler.getTrigger(triggerKey);
            if (trigger != null) {
                info.setCronExpression(trigger.getCronExpression());

                Date nextFireTime = trigger.getNextFireTime();
                if (nextFireTime != null) {
                    info.setNextFireTime(LocalDateTime.ofInstant(
                        nextFireTime.toInstant(), ZoneId.systemDefault()));
                }

                Date previousFireTime = trigger.getPreviousFireTime();
                if (previousFireTime != null) {
                    info.setPreviousFireTime(LocalDateTime.ofInstant(
                        previousFireTime.toInstant(), ZoneId.systemDefault()));
                }
            }
        } else {
            info.setScheduled(false);
        }

        return info;
    }

    /**
     * 获取所有已调度任务的ID列表
     */
    public List<Long> getAllScheduledTaskIds() throws SchedulerException {
        List<Long> ids = new ArrayList<>();
        for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.groupEquals(JOB_GROUP))) {
            String name = jobKey.getName();
            if (name.startsWith("JOB_")) {
                try {
                    ids.add(Long.parseLong(name.substring(4)));
                } catch (NumberFormatException ignored) {}
            }
        }
        return ids;
    }

    // ========== 扩展调度类型 ==========

    /**
     * API触发调度：通过HTTP回调触发任务
     */
    @Transactional(rollbackFor = Exception.class)
    public void scheduleByApiTrigger(Long taskId, Map<String, Object> apiConfig) {
        EtlSyncTask task = syncTaskService.getDetail(taskId);
        if (task == null) {
            throw EtlException.taskNotFound(taskId);
        }

        String apiUrl = (String) apiConfig.get("url");
        String method = (String) apiConfig.getOrDefault("method", "POST");
        String headers = apiConfig.get("headers") != null ? JSON.toJSONString(apiConfig.get("headers")) : "{}";

        // 保存API触发器配置到任务调度配置
        Map<String, Object> config = new HashMap<>();
        config.put("triggerType", "API");
        config.put("url", apiUrl);
        config.put("method", method);
        config.put("headers", headers);
        task.setScheduleConfig(JSON.toJSONString(config));
        task.setCronExpression(null);
        syncTaskService.updateById(task);

        log.info("配置API触发调度成功: taskId={}, url={}", taskId, apiUrl);
    }

    /**
     * 事件触发调度：通过事件监听触发任务
     */
    @Transactional(rollbackFor = Exception.class)
    public void scheduleByEventTrigger(Long taskId, Map<String, Object> eventConfig) {
        EtlSyncTask task = syncTaskService.getDetail(taskId);
        if (task == null) {
            throw EtlException.taskNotFound(taskId);
        }

        String eventType = (String) eventConfig.getOrDefault("eventType", "TASK_COMPLETED");
        String eventFilter = eventConfig.get("eventFilter") != null ? JSON.toJSONString(eventConfig.get("eventFilter")) : "{}";

        Map<String, Object> config = new HashMap<>();
        config.put("triggerType", "EVENT");
        config.put("eventType", eventType);
        config.put("eventFilter", eventFilter);
        task.setScheduleConfig(JSON.toJSONString(config));
        task.setCronExpression(null);
        syncTaskService.updateById(task);

        log.info("配置事件触发调度成功: taskId={}, eventType={}", taskId, eventType);
    }

    /**
     * 依赖触发调度：依赖任务完成后自动触发
     */
    @Transactional(rollbackFor = Exception.class)
    public void scheduleByDependencyTrigger(Long taskId, Map<String, Object> dependencyConfig) {
        EtlSyncTask task = syncTaskService.getDetail(taskId);
        if (task == null) {
            throw EtlException.taskNotFound(taskId);
        }

        @SuppressWarnings("unchecked")
        List<Long> dependsOnIds = (List<Long>) dependencyConfig.get("dependsOnIds");
        String dependencyType = (String) dependencyConfig.getOrDefault("type", "SUCCESS");

        if (dependsOnIds != null) {
            for (Long dependsOnId : dependsOnIds) {
                taskDependencyManager.addDependency(taskId, dependsOnId, dependencyType);
            }
        }

        Map<String, Object> config = new HashMap<>();
        config.put("triggerType", "DEPENDENCY");
        config.put("dependsOnIds", dependsOnIds);
        config.put("type", dependencyType);
        task.setScheduleConfig(JSON.toJSONString(config));
        task.setCronExpression(null);
        syncTaskService.updateById(task);

        log.info("配置依赖触发调度成功: taskId={}, dependsOnIds={}", taskId, dependsOnIds);
    }

    // ========== 调度预览 ==========

    /**
     * 获取调度预览（未来N次执行时间）
     */
    public Map<String, Object> getSchedulePreview(Long taskId, Integer count) throws SchedulerException {
        Map<String, Object> preview = new HashMap<>();
        preview.put("taskId", taskId);

        EtlSyncTask task = syncTaskService.getDetail(taskId);
        if (task == null) {
            throw EtlException.taskNotFound(taskId);
        }

        String cronExpression = task.getCronExpression();
        List<Map<String, Object>> fireTimes = new ArrayList<>();

        if (cronExpression != null && !cronExpression.isEmpty() && isValidCron(cronExpression)) {
            try {
                org.quartz.CronExpression cron = new org.quartz.CronExpression(cronExpression);
                java.util.Date nextTime = new java.util.Date();

                for (int i = 0; i < count; i++) {
                    nextTime = cron.getNextValidTimeAfter(nextTime);
                    if (nextTime == null) break;

                    Map<String, Object> fireTime = new LinkedHashMap<>();
                    fireTime.put("index", i + 1);
                    fireTime.put("time", LocalDateTime.ofInstant(
                        nextTime.toInstant(), ZoneId.systemDefault()));
                    fireTime.put("timestamp", nextTime.getTime());
                    fireTimes.add(fireTime);
                }
            } catch (java.text.ParseException e) {
                log.error("解析Cron表达式失败: {}", cronExpression, e);
            }
        }

        preview.put("cronExpression", cronExpression);
        preview.put("nextFireTimes", fireTimes);
        preview.put("count", fireTimes.size());
        return preview;
    }

    // ========== 工具方法 ==========

    /**
     * 校验并解析Cron表达式，返回人类可读的描述
     */
    public Map<String, Object> parseCronExpression(String cronExpression) {
        Map<String, Object> result = new HashMap<>();
        result.put("valid", isValidCron(cronExpression));
        result.put("expression", cronExpression);

        if (isValidCron(cronExpression)) {
            // 提供Cron表达式的语义描述
            List<String> descriptions = new ArrayList<>();
            String[] parts = cronExpression.trim().split("\\s+");
            if (parts.length >= 6) {
                String sec = parts[0];
                String min = parts[1];
                String hour = parts[2];
                String day = parts[3];
                String month = parts[4];
                String week = parts[5];

                if ("*".equals(min) && "*".equals(hour) && "*".equals(day) && "*".equals(month) && "?".equals(week)) {
                    descriptions.add("每" + ("*/1".equals(sec) || "*".equals(sec) ? "秒" : sec + "秒"));
                }
                if (!"*".equals(min) && !min.contains("/")) {
                    descriptions.add("第" + min + "分钟");
                }
                if (!"*".equals(hour)) {
                    descriptions.add(hour + "时");
                }
                if (!"*".equals(day) && !"?".equals(day)) {
                    descriptions.add("每月" + day + "日");
                }
                if (!"*".equals(month)) {
                    descriptions.add(month + "月");
                }
                if (!"*".equals(week) && !"?".equals(week)) {
                    String[] weekNames = {"", "周日", "周一", "周二", "周三", "周四", "周五", "周六"};
                    descriptions.add(weekNames.length > Integer.parseInt(week) ? weekNames[Integer.parseInt(week)] : "周" + week);
                }
            }
            result.put("description", String.join(" ", descriptions));
        }
        return result;
    }

    private JobKey getJobKey(Long taskId) {
        return new JobKey("JOB_" + taskId, JOB_GROUP);
    }

    private TriggerKey getTriggerKey(Long taskId) {
        return new TriggerKey("TRIGGER_" + taskId, TRIGGER_GROUP);
    }

    private boolean isValidCron(String cronExpression) {
        try {
            CronExpression.validateExpression(cronExpression);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
