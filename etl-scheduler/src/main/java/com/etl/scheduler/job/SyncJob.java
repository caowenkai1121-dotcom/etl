package com.etl.scheduler.job;

import com.etl.common.domain.SyncPipelineContext;
import com.etl.common.enums.SyncMode;
import com.etl.engine.SyncEngine;
import com.etl.engine.SyncEngineFactory;
import com.etl.engine.entity.EtlSyncTask;
import com.etl.engine.service.SyncTaskService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 同步任务Job
 */
@Slf4j
@DisallowConcurrentExecution
@PersistJobDataAfterExecution
public class SyncJob implements Job {

    @Autowired
    private SyncTaskService syncTaskService;

    @Autowired
    private SyncEngineFactory syncEngineFactory;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap dataMap = context.getJobDetail().getJobDataMap();
        Long taskId = dataMap.getLong("taskId");

        // 如果@Autowired未生效，从SchedulerContext回退获取
        if (syncTaskService == null) {
            try {
                syncTaskService = (SyncTaskService) context.getScheduler()
                    .getContext().get("syncTaskService");
            } catch (SchedulerException e) {
                log.error("获取syncTaskService失败", e);
                return;
            }
        }
        if (syncEngineFactory == null) {
            try {
                syncEngineFactory = (SyncEngineFactory) context.getScheduler()
                    .getContext().get("syncEngineFactory");
            } catch (SchedulerException e) {
                log.error("获取syncEngineFactory失败", e);
                return;
            }
        }

        try {
            EtlSyncTask task = syncTaskService.getDetail(taskId);
            if (task == null) {
                log.warn("任务不存在: taskId={}", taskId);
                return;
            }

            if (task.getSyncMode() == null) {
                log.error("任务同步模式为空: taskId={}", taskId);
                return;
            }

            log.info("开始执行定时同步任务: taskId={}, name={}", taskId, task.getName());

            // 构建同步上下文
            SyncPipelineContext syncContext = new SyncPipelineContext();
            syncContext.setTaskId(taskId);
            syncContext.setSourceDsId(task.getSourceDsId());
            syncContext.setTargetDsId(task.getTargetDsId());
            syncContext.setBatchSize(task.getBatchSize() != null ? task.getBatchSize() : 1000);

            // 获取同步引擎
            SyncMode syncMode = SyncMode.fromCode(task.getSyncMode());
            if (syncMode == null) {
                log.error("无效的同步模式: taskId={}, syncMode={}", taskId, task.getSyncMode());
                return;
            }
            SyncEngine engine = syncEngineFactory.getEngine(syncMode);

            // 执行同步
            engine.sync(syncContext);

            log.info("定时同步任务执行完成: taskId={}", taskId);

        } catch (Exception e) {
            log.error("定时同步任务执行失败: taskId={}", taskId, e);
            throw new JobExecutionException(e);
        }
    }
}
