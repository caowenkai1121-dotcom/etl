package com.etl.monitor.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.etl.common.enums.ExecutionStatus;
import com.etl.engine.entity.EtlSyncLog;
import com.etl.engine.entity.EtlTaskExecution;
import com.etl.engine.mapper.SyncLogMapper;
import com.etl.engine.mapper.TaskExecutionMapper;
import com.etl.engine.dto.TaskExecutionResponse;
import com.etl.monitor.dto.ExecutionTrendResponse;
import com.etl.monitor.dto.SystemOverviewResponse;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 监控服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MonitorService {

    private final TaskExecutionMapper executionMapper;
    private final SyncLogMapper logMapper;

    // 系统概览数据缓存（TTL 30秒）
    private final Cache<String, SystemOverviewResponse> overviewCache = Caffeine.newBuilder()
        .expireAfterWrite(30, TimeUnit.SECONDS)
        .maximumSize(1)
        .build();

    // 执行趋势数据缓存（TTL 5分钟）
    private final Cache<Integer, List<ExecutionTrendResponse>> trendCache = Caffeine.newBuilder()
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .maximumSize(5)
        .build();

    /**
     * 获取系统概览统计
     */
    public SystemOverviewResponse getSystemOverview() {
        return overviewCache.get("system_overview", key -> {
            SystemOverviewResponse overview = new SystemOverviewResponse();

            LocalDateTime todayStart = LocalDate.now().atStartOfDay();
            LambdaQueryWrapper<EtlTaskExecution> todayWrapper = new LambdaQueryWrapper<>();
            todayWrapper.ge(EtlTaskExecution::getStartTime, todayStart);

            List<EtlTaskExecution> todayExecutions = executionMapper.selectList(todayWrapper);
            overview.setTodayExecutions(todayExecutions.size());
            overview.setTodaySuccess((int) todayExecutions.stream()
                .filter(e -> ExecutionStatus.SUCCESS.getCode().equals(e.getStatus()))
                .count());
            overview.setTodayFailed((int) todayExecutions.stream()
                .filter(e -> ExecutionStatus.FAILED.getCode().equals(e.getStatus()))
                .count());

            LambdaQueryWrapper<EtlTaskExecution> runningWrapper = new LambdaQueryWrapper<>();
            runningWrapper.eq(EtlTaskExecution::getStatus, ExecutionStatus.RUNNING.getCode());
            overview.setRunningTasks(Math.toIntExact(executionMapper.selectCount(runningWrapper)));

            long totalRows = todayExecutions.stream()
                .mapToLong(e -> e.getSuccessRows() != null ? e.getSuccessRows() : 0L)
                .sum();
            overview.setTodayTotalRows(totalRows);

            return overview;
        });
    }

    /**
     * 获取任务执行趋势
     */
    public List<ExecutionTrendResponse> getExecutionTrend(int days) {
        return trendCache.get(days, key -> {
            List<ExecutionTrendResponse> trends = new ArrayList<>();
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(days - 1);

            for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                LocalDateTime dayStart = date.atStartOfDay();
                LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();

                LambdaQueryWrapper<EtlTaskExecution> wrapper = new LambdaQueryWrapper<>();
                wrapper.ge(EtlTaskExecution::getStartTime, dayStart)
                       .lt(EtlTaskExecution::getStartTime, dayEnd);

                List<EtlTaskExecution> executions = executionMapper.selectList(wrapper);

                ExecutionTrendResponse trend = new ExecutionTrendResponse();
                trend.setDate(date.toString());
                trend.setTotal(executions.size());
                trend.setSuccess((int) executions.stream()
                    .filter(e -> ExecutionStatus.SUCCESS.getCode().equals(e.getStatus()))
                    .count());
                trend.setFailed((int) executions.stream()
                    .filter(e -> ExecutionStatus.FAILED.getCode().equals(e.getStatus()))
                    .count());
                trend.setTotalRows(executions.stream()
                    .mapToLong(e -> e.getSuccessRows() != null ? e.getSuccessRows() : 0L)
                    .sum());

                trends.add(trend);
            }

            return trends;
        });
    }

    /**
     * 获取任务执行详情
     */
    public TaskExecutionResponse getExecutionDetail(Long executionId) {
        EtlTaskExecution execution = executionMapper.selectById(executionId);
        if (execution == null) {
            return null;
        }
        return TaskExecutionResponse.from(execution);
    }

    /**
     * 清理历史日志
     */
    @Transactional(rollbackFor = Exception.class)
    public int cleanHistoryLogs(int retentionDays) {
        LocalDateTime threshold = LocalDateTime.now().minusDays(retentionDays);

        LambdaQueryWrapper<EtlTaskExecution> execWrapper = new LambdaQueryWrapper<>();
        execWrapper.lt(EtlTaskExecution::getStartTime, threshold)
                   .ne(EtlTaskExecution::getStatus, ExecutionStatus.RUNNING.getCode());
        int execDeleted = executionMapper.delete(execWrapper);

        LambdaQueryWrapper<EtlSyncLog> logWrapper = new LambdaQueryWrapper<>();
        logWrapper.lt(EtlSyncLog::getCreatedAt, threshold);
        int logDeleted = logMapper.delete(logWrapper);

        log.info("清理历史数据完成: 执行记录{}条, 日志{}条", execDeleted, logDeleted);

        // 清理缓存（确保下次获取最新数据）
        overviewCache.invalidateAll();
        trendCache.invalidateAll();

        return execDeleted + logDeleted;
    }
}
