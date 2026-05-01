package com.etl.engine.service;

import com.etl.common.domain.SyncPipelineContext;
import com.etl.engine.SyncEngine;
import com.etl.engine.dto.TaskProgressResponse;
import com.etl.engine.entity.EtlTaskExecution;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 任务执行管理器
 * 跟踪正在执行的任务，支持停止和进度查询
 */
@Slf4j
@Service
public class TaskExecutionManager {

    /**
     * 正在执行的任务: taskId -> ExecutionInfo
     */
    private final Map<Long, ExecutionInfo> runningExecutions = new ConcurrentHashMap<>();

    /**
     * 开始执行
     */
    public void startExecution(Long taskId, SyncEngine engine, SyncPipelineContext context, EtlTaskExecution execution) {
        ExecutionInfo info = new ExecutionInfo(taskId, engine, context, execution);
        runningExecutions.put(taskId, info);
        log.info("开始跟踪任务执行: taskId={}", taskId);
    }

    /**
     * 结束执行
     */
    public void endExecution(Long taskId) {
        ExecutionInfo info = runningExecutions.remove(taskId);
        if (info != null) {
            log.info("结束跟踪任务执行: taskId={}", taskId);
        }
    }

    /**
     * 停止任务
     */
    public boolean stopTask(Long taskId) {
        ExecutionInfo info = runningExecutions.get(taskId);
        if (info != null && info.isRunning()) {
            log.info("正在停止任务: taskId={}", taskId);
            info.getEngine().stop();
            info.getStopped().set(true);
            return true;
        }
        return false;
    }

    /**
     * 获取任务进度
     */
    public TaskProgressResponse getProgress(Long taskId) {
        ExecutionInfo info = runningExecutions.get(taskId);
        if (info != null) {
            SyncPipelineContext context = info.getContext();
            EtlTaskExecution execution = info.getExecution();

            TaskProgressResponse progressInfo = new TaskProgressResponse();
            progressInfo.setTaskId(taskId);
            progressInfo.setExecutionId(execution.getId());
            progressInfo.setExecutionNo(execution.getExecutionNo());
            progressInfo.setProgress(info.getEngine().getProgress());
            progressInfo.setTotalRows(context.getTotalRows());
            progressInfo.setSuccessRows(context.getSuccessRows());
            progressInfo.setFailedRows(context.getFailedRows());
            progressInfo.setRunning(info.isRunning());
            progressInfo.setStatus(info.isRunning() ? "RUNNING" : "STOPPED");
            return progressInfo;
        }
        return null;
    }

    /**
     * 是否正在执行
     */
    public boolean isRunning(Long taskId) {
        ExecutionInfo info = runningExecutions.get(taskId);
        return info != null && info.isRunning();
    }

    /**
     * 获取所有正在执行的任务
     */
    public Map<Long, ExecutionInfo> getRunningExecutions() {
        return runningExecutions;
    }

    /**
     * 执行信息
     */
    @Getter
    public static class ExecutionInfo {
        private final Long taskId;
        private final SyncEngine engine;
        private final SyncPipelineContext context;
        private final EtlTaskExecution execution;
        private final AtomicBoolean stopped = new AtomicBoolean(false);
        private final long startTime;

        public ExecutionInfo(Long taskId, SyncEngine engine, SyncPipelineContext context, EtlTaskExecution execution) {
            this.taskId = taskId;
            this.engine = engine;
            this.context = context;
            this.execution = execution;
            this.startTime = System.currentTimeMillis();
        }

        public boolean isRunning() {
            return engine.isRunning() && !stopped.get();
        }

        public long getDuration() {
            return System.currentTimeMillis() - startTime;
        }
    }

}
