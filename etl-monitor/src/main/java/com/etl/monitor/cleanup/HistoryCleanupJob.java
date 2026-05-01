package com.etl.monitor.cleanup;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.etl.common.enums.ExecutionStatus;
import com.etl.engine.entity.EtlFailedTaskArchive;
import com.etl.engine.entity.EtlSyncLog;
import com.etl.engine.entity.EtlTaskExecution;
import com.etl.engine.mapper.FailedTaskArchiveMapper;
import com.etl.engine.mapper.SyncLogMapper;
import com.etl.engine.mapper.TaskExecutionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 历史数据清理任务
 * 定时任务，每天凌晨2点执行，清理过期的任务执行记录、日志等
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HistoryCleanupJob {

    private final TaskExecutionMapper taskExecutionMapper;
    private final SyncLogMapper syncLogMapper;
    private final FailedTaskArchiveMapper failedTaskArchiveMapper;

    // 默认保留天数
    private static final int SUCCESS_EXECUTION_KEEP_DAYS = 90;
    private static final int FAILED_EXECUTION_KEEP_DAYS = 180;
    private static final int LOG_KEEP_DAYS = 30;
    private static final int BATCH_SIZE = 5000;

    /**
     * 每天凌晨2点执行历史数据清理任务
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupDaily() {
        log.info("开始执行历史数据清理任务");
        cleanup();
        log.info("历史数据清理任务执行完成");
    }

    /**
     * 手动触发历史数据清理任务
     */
    public void cleanup() {
        try {
            // 清理成功执行记录（保留90天）
            cleanupSuccessExecutions();

            // 清理失败执行记录（先归档，再删除，保留180天）
            cleanupFailedExecutions();

            // 清理日志记录（保留30天）
            cleanupSyncLogs();

        } catch (Exception e) {
            log.error("历史数据清理任务执行异常", e);
            throw new RuntimeException("历史数据清理失败", e);
        }
    }

    /**
     * 清理成功执行记录
     */
    @Transactional
    protected void cleanupSuccessExecutions() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(SUCCESS_EXECUTION_KEEP_DAYS);
        log.info("清理 {} 之前的成功执行记录", cutoff);

        int totalDeleted = 0;
        int deleted;
        do {
            // 分批查询需要删除的记录
            LambdaQueryWrapper<EtlTaskExecution> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(EtlTaskExecution::getStatus, ExecutionStatus.SUCCESS.getCode())
                    .lt(EtlTaskExecution::getCreatedAt, cutoff)
                    .last("LIMIT " + BATCH_SIZE);
            List<EtlTaskExecution> executions = taskExecutionMapper.selectList(queryWrapper);

            if (executions.isEmpty()) {
                break;
            }

            // 删除记录
            List<Long> ids = executions.stream().map(EtlTaskExecution::getId).toList();
            LambdaQueryWrapper<EtlTaskExecution> deleteWrapper = new LambdaQueryWrapper<>();
            deleteWrapper.in(EtlTaskExecution::getId, ids);
            deleted = taskExecutionMapper.delete(deleteWrapper);
            totalDeleted += deleted;

        } while (deleted > 0);

        log.info("成功执行记录清理完成，共删除 {} 条", totalDeleted);
    }

    /**
     * 清理失败执行记录（先归档再删除）
     */
    @Transactional
    protected void cleanupFailedExecutions() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(FAILED_EXECUTION_KEEP_DAYS);
        log.info("清理 {} 之前的失败执行记录", cutoff);

        int totalArchived = 0;
        int totalDeleted = 0;
        int batchCount;

        do {
            // 分批查询需要归档删除的记录
            LambdaQueryWrapper<EtlTaskExecution> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(EtlTaskExecution::getStatus, ExecutionStatus.FAILED.getCode())
                    .lt(EtlTaskExecution::getCreatedAt, cutoff)
                    .last("LIMIT " + BATCH_SIZE);
            List<EtlTaskExecution> executions = taskExecutionMapper.selectList(queryWrapper);

            if (executions.isEmpty()) {
                break;
            }

            // 归档记录
            List<EtlFailedTaskArchive> archives = new ArrayList<>();
            for (EtlTaskExecution execution : executions) {
                EtlFailedTaskArchive archive = new EtlFailedTaskArchive();
                archive.setOriginalExecutionId(execution.getId());
                archive.setTaskId(execution.getTaskId());
                archive.setExecutionNo(execution.getExecutionNo());
                archive.setTriggerType(execution.getTriggerType());
                archive.setStartTime(execution.getStartTime());
                archive.setEndTime(execution.getEndTime());
                archive.setDuration(execution.getDuration());
                archive.setStatus(execution.getStatus());
                archive.setTotalRows(execution.getTotalRows());
                archive.setSuccessRows(execution.getSuccessRows());
                archive.setFailedRows(execution.getFailedRows());
                archive.setSkipRows(execution.getSkipRows());
                archive.setErrorMessage(execution.getErrorMessage());
                archive.setCheckpoint(execution.getCheckpoint());
                archive.setProgress(execution.getProgress());
                archive.setArchivedAt(LocalDateTime.now());
                archives.add(archive);
            }

            for (EtlFailedTaskArchive archive : archives) {
                failedTaskArchiveMapper.insert(archive);
            }
            totalArchived += archives.size();

            // 删除原记录
            List<Long> ids = executions.stream().map(EtlTaskExecution::getId).toList();
            LambdaQueryWrapper<EtlTaskExecution> deleteWrapper = new LambdaQueryWrapper<>();
            deleteWrapper.in(EtlTaskExecution::getId, ids);
            batchCount = taskExecutionMapper.delete(deleteWrapper);
            totalDeleted += batchCount;

        } while (batchCount > 0);

        log.info("失败执行记录清理完成，共归档 {} 条，删除 {} 条", totalArchived, totalDeleted);
    }

    /**
     * 清理同步日志记录
     */
    @Transactional
    protected void cleanupSyncLogs() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(LOG_KEEP_DAYS);
        log.info("清理 {} 之前的同步日志记录", cutoff);

        int totalDeleted = 0;
        int deleted;
        do {
            // 分批查询需要删除的记录
            LambdaQueryWrapper<EtlSyncLog> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.lt(EtlSyncLog::getCreatedAt, cutoff)
                    .last("LIMIT " + BATCH_SIZE);
            List<EtlSyncLog> logs = syncLogMapper.selectList(queryWrapper);

            if (logs.isEmpty()) {
                break;
            }

            // 删除记录
            List<Long> ids = logs.stream().map(EtlSyncLog::getId).toList();
            LambdaQueryWrapper<EtlSyncLog> deleteWrapper = new LambdaQueryWrapper<>();
            deleteWrapper.in(EtlSyncLog::getId, ids);
            deleted = syncLogMapper.delete(deleteWrapper);
            totalDeleted += deleted;

        } while (deleted > 0);

        log.info("同步日志记录清理完成，共删除 {} 条", totalDeleted);
    }
}
