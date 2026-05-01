package com.etl.monitor.statistics;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.etl.common.enums.ExecutionStatus;
import com.etl.engine.entity.EtlTaskExecution;
import com.etl.engine.mapper.TaskExecutionMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 性能统计服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final TaskExecutionMapper executionMapper;

    /**
     * 获取每日同步统计
     */
    public DailyStatistics getDailyStatistics(LocalDate date) {
        DailyStatistics stats = new DailyStatistics();
        stats.setDate(date);

        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();

        LambdaQueryWrapper<EtlTaskExecution> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(EtlTaskExecution::getStartTime, dayStart)
               .lt(EtlTaskExecution::getStartTime, dayEnd);

        List<EtlTaskExecution> executions = executionMapper.selectList(wrapper);

        stats.setTotalExecutions(executions.size());
        stats.setSuccessCount((int) executions.stream()
            .filter(e -> ExecutionStatus.SUCCESS.getCode().equals(e.getStatus()))
            .count());
        stats.setFailedCount((int) executions.stream()
            .filter(e -> ExecutionStatus.FAILED.getCode().equals(e.getStatus()))
            .count());

        long totalRows = executions.stream()
            .mapToLong(e -> e.getTotalRows() != null ? e.getTotalRows() : 0L)
            .sum();
        long successRows = executions.stream()
            .mapToLong(e -> e.getSuccessRows() != null ? e.getSuccessRows() : 0L)
            .sum();

        stats.setTotalRows(totalRows);
        stats.setSuccessRows(successRows);

        if (totalRows > 0) {
            stats.setSuccessRate(new BigDecimal(successRows)
                .divide(new BigDecimal(totalRows), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal(100)));
        }

        // 计算平均耗时
        double avgDuration = executions.stream()
            .filter(e -> e.getDuration() != null)
            .mapToLong(EtlTaskExecution::getDuration)
            .average()
            .orElse(0);
        stats.setAvgDuration((long) avgDuration);

        return stats;
    }

    /**
     * 获取日期范围内的统计趋势
     */
    public List<DailyStatistics> getStatisticsTrend(LocalDate startDate, LocalDate endDate) {
        List<DailyStatistics> trends = new ArrayList<>();

        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            trends.add(getDailyStatistics(current));
            current = current.plusDays(1);
        }

        return trends;
    }

    /**
     * 获取任务执行成功率趋势
     */
    public List<SuccessRateTrend> getSuccessRateTrend(int days) {
        List<SuccessRateTrend> trends = new ArrayList<>();
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            DailyStatistics stats = getDailyStatistics(date);

            SuccessRateTrend trend = new SuccessRateTrend();
            trend.setDate(date.toString());
            trend.setTotalExecutions(stats.getTotalExecutions());
            trend.setSuccessCount(stats.getSuccessCount());
            trend.setFailedCount(stats.getFailedCount());
            trend.setSuccessRate(stats.getSuccessRate());

            trends.add(trend);
        }

        return trends;
    }

    /**
     * 获取任务性能排行
     */
    public List<TaskPerformance> getTaskPerformanceRank(int limit, LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();

        LambdaQueryWrapper<EtlTaskExecution> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(EtlTaskExecution::getStartTime, start)
               .lt(EtlTaskExecution::getStartTime, end)
               .orderByDesc(EtlTaskExecution::getSuccessRows);

        List<EtlTaskExecution> executions = executionMapper.selectList(wrapper);

        // 按任务ID分组统计
        Map<Long, TaskPerformance> performanceMap = new HashMap<>();

        for (EtlTaskExecution execution : executions) {
            Long taskId = execution.getTaskId();
            TaskPerformance perf = performanceMap.computeIfAbsent(taskId, id -> {
                TaskPerformance p = new TaskPerformance();
                p.setTaskId(id);
                return p;
            });

            perf.setExecutionCount(perf.getExecutionCount() + 1);
            perf.setTotalRows(perf.getTotalRows() + (execution.getTotalRows() != null ? execution.getTotalRows() : 0L));
            perf.setSuccessRows(perf.getSuccessRows() + (execution.getSuccessRows() != null ? execution.getSuccessRows() : 0L));
            perf.setFailedRows(perf.getFailedRows() + (execution.getFailedRows() != null ? execution.getFailedRows() : 0L));
        }

        // 排序并返回
        return performanceMap.values().stream()
            .sorted((a, b) -> Long.compare(b.getSuccessRows(), a.getSuccessRows()))
            .limit(limit)
            .toList();
    }

    /**
     * 获取系统性能概览
     */
    public SystemPerformanceOverview getSystemPerformanceOverview() {
        SystemPerformanceOverview overview = new SystemPerformanceOverview();

        // 今日统计
        DailyStatistics today = getDailyStatistics(LocalDate.now());
        overview.setTodayExecutions(today.getTotalExecutions());
        overview.setTodaySuccessRows(today.getSuccessRows());
        overview.setTodaySuccessRate(today.getSuccessRate());

        // 本周统计
        LocalDate weekStart = LocalDate.now().minusDays(6);
        List<DailyStatistics> weekStats = getStatisticsTrend(weekStart, LocalDate.now());

        long weekTotalRows = weekStats.stream()
            .mapToLong(DailyStatistics::getTotalRows)
            .sum();
        long weekSuccessRows = weekStats.stream()
            .mapToLong(DailyStatistics::getSuccessRows)
            .sum();

        overview.setWeekTotalRows(weekTotalRows);
        overview.setWeekSuccessRows(weekSuccessRows);

        // 计算平均每日同步行数
        overview.setAvgDailyRows(weekSuccessRows / 7);

        return overview;
    }

    // ==================== 内部类 ====================

    @Data
    public static class DailyStatistics {
        private LocalDate date;
        private int totalExecutions;
        private int successCount;
        private int failedCount;
        private long totalRows;
        private long successRows;
        private BigDecimal successRate;
        private long avgDuration;
    }

    @Data
    public static class SuccessRateTrend {
        private String date;
        private int totalExecutions;
        private int successCount;
        private int failedCount;
        private BigDecimal successRate;
    }

    @Data
    public static class TaskPerformance {
        private Long taskId;
        private String taskName;
        private int executionCount;
        private long totalRows;
        private long successRows;
        private long failedRows;
    }

    @Data
    public static class SystemPerformanceOverview {
        private int todayExecutions;
        private long todaySuccessRows;
        private BigDecimal todaySuccessRate;
        private long weekTotalRows;
        private long weekSuccessRows;
        private long avgDailyRows;
    }
}
