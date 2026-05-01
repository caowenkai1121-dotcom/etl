package com.etl.engine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.etl.common.enums.ExecutionStatus;
import com.etl.common.utils.CodeGenerator;
import com.etl.engine.entity.EtlTaskExecution;
import com.etl.engine.mapper.TaskExecutionMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 任务执行记录服务
 */
@Slf4j
@Service
public class TaskExecutionService extends ServiceImpl<TaskExecutionMapper, EtlTaskExecution> {

    /**
     * 创建执行记录
     */
    @Transactional(rollbackFor = Exception.class)
    public EtlTaskExecution createExecution(Long taskId, String triggerType) {
        EtlTaskExecution execution = new EtlTaskExecution();
        execution.setTaskId(taskId);
        execution.setExecutionNo(CodeGenerator.generateExecutionNo());
        execution.setTriggerType(triggerType);
        execution.setStartTime(LocalDateTime.now());
        execution.setStatus(ExecutionStatus.RUNNING.getCode());
        execution.setTotalRows(0L);
        execution.setSuccessRows(0L);
        execution.setFailedRows(0L);
        execution.setSkipRows(0L);
        execution.setProgress(BigDecimal.ZERO);
        save(execution);
        log.info("创建执行记录: taskId={}, executionNo={}", taskId, execution.getExecutionNo());
        return execution;
    }

    /**
     * 更新执行状态
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, String status) {
        EtlTaskExecution execution = getById(id);
        if (execution != null) {
            execution.setStatus(status);
            if (ExecutionStatus.SUCCESS.getCode().equals(status) ||
                ExecutionStatus.FAILED.getCode().equals(status) ||
                ExecutionStatus.CANCELLED.getCode().equals(status)) {
                execution.setEndTime(LocalDateTime.now());
                execution.setDuration(java.time.Duration.between(
                    execution.getStartTime(), execution.getEndTime()).toMillis());
            }
            updateById(execution);
        }
    }

    /**
     * 更新执行进度
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateProgress(Long id, long totalRows, long successRows, long failedRows, BigDecimal progress) {
        EtlTaskExecution execution = getById(id);
        if (execution != null) {
            execution.setTotalRows(totalRows);
            execution.setSuccessRows(successRows);
            execution.setFailedRows(failedRows);
            execution.setProgress(progress);
            updateById(execution);
        }
    }

    /**
     * 完成执行
     */
    @Transactional(rollbackFor = Exception.class)
    public void completeExecution(Long id, String status, String errorMessage, String checkpoint) {
        EtlTaskExecution execution = getById(id);
        if (execution != null) {
            execution.setStatus(status);
            execution.setEndTime(LocalDateTime.now());
            execution.setDuration(java.time.Duration.between(
                execution.getStartTime(), execution.getEndTime()).toMillis());
            execution.setErrorMessage(errorMessage);
            execution.setCheckpoint(checkpoint);
            if (execution.getTotalRows() > 0) {
                execution.setProgress(new BigDecimal("100.00"));
            }
            updateById(execution);
            log.info("执行完成: executionId={}, status={}, duration={}ms",
                id, status, execution.getDuration());
        }
    }

    /**
     * 分页查询执行记录
     */
    public Page<EtlTaskExecution> pageList(Integer pageNum, Integer pageSize, Long taskId, String status) {
        LambdaQueryWrapper<EtlTaskExecution> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(taskId != null, EtlTaskExecution::getTaskId, taskId)
               .eq(status != null && !status.isEmpty(), EtlTaskExecution::getStatus, status)
               .orderByDesc(EtlTaskExecution::getStartTime);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    /**
     * 获取任务最近的执行记录
     */
    public EtlTaskExecution getLatestExecution(Long taskId) {
        LambdaQueryWrapper<EtlTaskExecution> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EtlTaskExecution::getTaskId, taskId)
               .orderByDesc(EtlTaskExecution::getStartTime)
               .last("LIMIT 1");
        return getOne(wrapper);
    }

    /**
     * 批量获取多个任务的最新执行记录（一条SQL，避免N+1）
     */
    public Map<Long, EtlTaskExecution> getLatestExecutionsBatch(List<Long> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<EtlTaskExecution> list = ((TaskExecutionMapper) getBaseMapper()).selectLatestByTaskIds(taskIds);
        Map<Long, EtlTaskExecution> result = new LinkedHashMap<>();
        for (EtlTaskExecution e : list) {
            result.put(e.getTaskId(), e);
        }
        return result;
    }

    /**
     * 获取运行中的执行记录
     */
    public List<EtlTaskExecution> getRunningExecutions(Long taskId) {
        LambdaQueryWrapper<EtlTaskExecution> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(taskId != null, EtlTaskExecution::getTaskId, taskId)
               .eq(EtlTaskExecution::getStatus, ExecutionStatus.RUNNING.getCode());
        return list(wrapper);
    }
}
