package com.etl.engine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.etl.common.enums.TaskStatus;
import com.etl.common.exception.EtlException;
import com.etl.engine.cdc.CdcManagerService;
import com.etl.engine.entity.EtlSyncTask;
import com.etl.engine.mapper.SyncTaskMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 同步任务服务
 */
@Slf4j
@Service
public class SyncTaskService extends ServiceImpl<SyncTaskMapper, EtlSyncTask> {

    @Autowired
    @Lazy
    private CdcManagerService cdcManagerService;

    /**
     * 分页查询任务
     */
    public Page<EtlSyncTask> pageList(Integer pageNum, Integer pageSize, String name, String status) {
        LambdaQueryWrapper<EtlSyncTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(name != null && !name.isEmpty(), EtlSyncTask::getName, name)
               .eq(status != null && !status.isEmpty(), EtlSyncTask::getStatus, status)
               .orderByDesc(EtlSyncTask::getCreatedAt);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    /**
     * 创建任务
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createTask(EtlSyncTask task) {
        task.setStatus(TaskStatus.CREATED.getCode());
        // 处理空的JSON字段，避免MySQL JSON类型存储空字符串报错
        if (task.getFieldMapping() == null || task.getFieldMapping().isBlank()) {
            task.setFieldMapping("[]");
        }
        if (task.getTableConfig() == null || task.getTableConfig().isBlank()) {
            task.setTableConfig("[]");
        }
        save(task);
        log.info("创建同步任务成功: id={}, name={}", task.getId(), task.getName());
        return task.getId();
    }

    /**
     * 更新任务
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateTask(EtlSyncTask task) {
        EtlSyncTask existing = getById(task.getId());
        if (existing == null) {
            throw EtlException.taskNotFound(task.getId());
        }
        updateById(task);
        log.info("更新同步任务成功: id={}", task.getId());
    }

    /**
     * 删除任务
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteTask(Long id) {
        EtlSyncTask task = getById(id);
        if (task == null) {
            throw EtlException.taskNotFound(id);
        }
        // 检查任务是否实际在运行（特别是CDC任务）
        if (TaskStatus.RUNNING.getCode().equals(task.getStatus())) {
            // 对于CDC模式，检查是否实际在运行
            if ("CDC".equals(task.getSyncMode()) && cdcManagerService.isRunning(id)) {
                throw EtlException.invalidTaskStatus(task.getStatus(), "请先停止任务后再删除");
            }
            // 非CDC模式或CDC任务已停止但状态未更新，允许删除并将状态更新为已停止
            task.setStatus(TaskStatus.STOPPED.getCode());
            updateById(task);
            log.info("任务状态已更新为STOPPED: id={}", id);
        }
        removeById(id);
        log.info("删除同步任务成功: id={}", id);
    }

    /**
     * 获取任务详情
     */
    public EtlSyncTask getDetail(Long id) {
        EtlSyncTask task = getById(id);
        if (task == null) {
            throw EtlException.taskNotFound(id);
        }
        return task;
    }

    /**
     * 更新任务状态
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, String status) {
        EtlSyncTask task = getById(id);
        if (task == null) {
            throw EtlException.taskNotFound(id);
        }
        task.setStatus(status);
        if (TaskStatus.RUNNING.getCode().equals(status)) {
            task.setLastSyncTime(LocalDateTime.now());
        }
        updateById(task);
        log.info("更新任务状态: id={}, status={}", id, status);
    }

    /**
     * 更新同步时间
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateSyncTime(Long id, LocalDateTime nextSyncTime) {
        EtlSyncTask task = getById(id);
        if (task != null) {
            task.setLastSyncTime(LocalDateTime.now());
            task.setNextSyncTime(nextSyncTime);
            updateById(task);
        }
    }

    /**
     * 更新增量值
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateIncrementalValue(Long id, String value) {
        EtlSyncTask task = getById(id);
        if (task != null) {
            task.setIncrementalValue(value);
            updateById(task);
        }
    }

    /**
     * 获取运行中的任务
     */
    public List<EtlSyncTask> getRunningTasks() {
        LambdaQueryWrapper<EtlSyncTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EtlSyncTask::getStatus, TaskStatus.RUNNING.getCode());
        return list(wrapper);
    }

    /**
     * 获取所有启用的任务
     */
    public List<EtlSyncTask> getEnabledTasks() {
        LambdaQueryWrapper<EtlSyncTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(EtlSyncTask::getStatus, TaskStatus.CREATED.getCode(), TaskStatus.RUNNING.getCode());
        return list(wrapper);
    }

    /**
     * 获取某个数据源下所有CDC任务
     */
    public List<EtlSyncTask> getCdcTasksByDatasourceId(Long datasourceId) {
        LambdaQueryWrapper<EtlSyncTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EtlSyncTask::getSourceDsId, datasourceId)
               .eq(EtlSyncTask::getSyncMode, "CDC")
               .in(EtlSyncTask::getStatus, TaskStatus.CREATED.getCode(), TaskStatus.RUNNING.getCode());
        return list(wrapper);
    }
}
