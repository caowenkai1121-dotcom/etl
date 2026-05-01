package com.etl.monitor.statistics;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.etl.common.enums.ExecutionStatus;
import com.etl.engine.entity.EtlTaskExecution;
import com.etl.engine.entity.EtlTaskSummary;
import com.etl.engine.mapper.TaskExecutionMapper;
import com.etl.engine.mapper.TaskSummaryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 统计数据聚合器
 * 定时任务，每天凌晨1点执行，聚合前一天的任务执行数据到任务摘要表
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StatisticsAggregator {

    private final TaskExecutionMapper taskExecutionMapper;
    private final TaskSummaryMapper taskSummaryMapper;

    /**
     * 每天凌晨1点执行统计聚合任务
     */
    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    public void aggregateDaily() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        log.info("开始聚合 {} 的任务执行统计数据", yesterday);
        aggregate(yesterday);
        log.info("完成 {} 的任务执行统计数据聚合", yesterday);
    }

    /**
     * 手动触发统计聚合任务
     * @param date 要聚合的日期
     */
    @Transactional
    public void aggregate(LocalDate date) {
        // 查询该日期的所有任务执行记录
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        // 查询该日期范围内的所有执行记录
        LambdaQueryWrapper<EtlTaskExecution> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(EtlTaskExecution::getStartTime, startOfDay)
                .le(EtlTaskExecution::getStartTime, endOfDay);
        List<EtlTaskExecution> executions = taskExecutionMapper.selectList(wrapper);

        if (executions.isEmpty()) {
            log.info("{} 没有任务执行记录", date);
            return;
        }

        // 按任务ID分组
        Map<Long, List<EtlTaskExecution>> groupedByTaskId = executions.stream()
                .collect(Collectors.groupingBy(EtlTaskExecution::getTaskId));

        // 先删除该日期已有的统计数据
        LambdaQueryWrapper<EtlTaskSummary> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(EtlTaskSummary::getSummaryDate, date);
        taskSummaryMapper.delete(deleteWrapper);

        // 聚合每个任务的统计数据
        List<EtlTaskSummary> summaries = new ArrayList<>();
        for (Map.Entry<Long, List<EtlTaskExecution>> entry : groupedByTaskId.entrySet()) {
            Long taskId = entry.getKey();
            List<EtlTaskExecution> taskExecutions = entry.getValue();

            EtlTaskSummary summary = aggregateTask(taskId, date, taskExecutions);
            summaries.add(summary);
            taskSummaryMapper.insert(summary);
        }

        log.info("{} 共聚合了 {} 个任务的统计数据", date, summaries.size());
    }

    /**
     * 聚合单个任务的统计数据
     */
    private EtlTaskSummary aggregateTask(Long taskId, LocalDate date, List<EtlTaskExecution> executions) {
        EtlTaskSummary summary = new EtlTaskSummary();
        summary.setTaskId(taskId);
        summary.setSummaryDate(date);
        summary.setCreatedAt(LocalDateTime.now());

        int totalExecutions = executions.size();
        int successExecutions = 0;
        int failedExecutions = 0;
        long totalDuration = 0;
        long totalRows = 0;
        long successRows = 0;
        long failedRows = 0;

        for (EtlTaskExecution execution : executions) {
            totalDuration += Optional.ofNullable(execution.getDuration()).orElse(0L);
            totalRows += Optional.ofNullable(execution.getTotalRows()).orElse(0L);
            successRows += Optional.ofNullable(execution.getSuccessRows()).orElse(0L);
            failedRows += Optional.ofNullable(execution.getFailedRows()).orElse(0L);

            if (ExecutionStatus.SUCCESS.getCode().equals(execution.getStatus())) {
                successExecutions++;
            } else if (ExecutionStatus.FAILED.getCode().equals(execution.getStatus())) {
                failedExecutions++;
            }
        }

        summary.setTotalExecutions(totalExecutions);
        summary.setSuccessExecutions(successExecutions);
        summary.setFailedExecutions(failedExecutions);
        summary.setAvgDuration(totalExecutions > 0 ? totalDuration / totalExecutions : 0L);
        summary.setTotalRows(totalRows);
        summary.setSuccessRows(successRows);
        summary.setFailedRows(failedRows);

        return summary;
    }
}
