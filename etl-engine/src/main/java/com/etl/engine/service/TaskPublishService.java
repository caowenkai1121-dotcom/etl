package com.etl.engine.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.etl.common.result.PageResult;
import com.etl.engine.entity.EtlTaskPublish;
import com.etl.engine.mapper.TaskPublishMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 任务发布服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskPublishService extends ServiceImpl<TaskPublishMapper, EtlTaskPublish> {

    /**
     * 分页查询发布记录
     */
    public PageResult<EtlTaskPublish> pageList(Integer pageNum, Integer pageSize, String publishStatus, String taskType) {
        Page<EtlTaskPublish> page = new Page<>(pageNum, pageSize);

        lambdaQuery()
            .eq(publishStatus != null && !publishStatus.isEmpty(), EtlTaskPublish::getPublishStatus, publishStatus)
            .eq(taskType != null && !taskType.isEmpty(), EtlTaskPublish::getTaskType, taskType)
            .orderByDesc(EtlTaskPublish::getCreateTime)
            .page(page);

        return PageResult.of(
            page.getRecords(),
            page.getTotal(),
            pageNum,
            pageSize
        );
    }

    /**
     * 发布任务
     */
    @Transactional
    public Long publishTask(Long taskId, String taskType, String changeLog, String snapshotConfig) {
        // 查询最新版本
        EtlTaskPublish lastPublish = lambdaQuery()
            .eq(EtlTaskPublish::getTaskId, taskId)
            .orderByDesc(EtlTaskPublish::getVersion)
            .last("LIMIT 1")
            .one();

        int version = lastPublish != null ? lastPublish.getVersion() + 1 : 1;

        EtlTaskPublish publish = new EtlTaskPublish();
        publish.setTaskId(taskId);
        publish.setTaskType(taskType != null ? taskType : "WORKFLOW");
        publish.setVersion(version);
        publish.setPublishStatus("PENDING");
        publish.setChangeLog(changeLog);
        publish.setSnapshotConfig(snapshotConfig);
        publish.setCreateTime(LocalDateTime.now());

        save(publish);
        return publish.getId();
    }

    /**
     * 审批通过
     */
    @Transactional
    public void approve(Long id, String approvedBy) {
        EtlTaskPublish publish = getById(id);
        if (publish == null) {
            throw new RuntimeException("发布记录不存在");
        }

        if (!"PENDING".equals(publish.getPublishStatus())) {
            throw new RuntimeException("只有待审批状态才能审批");
        }

        publish.setPublishStatus("PUBLISHED");
        publish.setPublishedBy(approvedBy);
        publish.setPublishedAt(LocalDateTime.now());
        updateById(publish);

        log.info("任务发布审批通过: id={}, taskId={}", id, publish.getTaskId());
    }

    /**
     * 审批拒绝
     */
    @Transactional
    public void reject(Long id, String rejectedBy, String reason) {
        EtlTaskPublish publish = getById(id);
        if (publish == null) {
            throw new RuntimeException("发布记录不存在");
        }

        if (!"PENDING".equals(publish.getPublishStatus())) {
            throw new RuntimeException("只有待审批状态才能拒绝");
        }

        publish.setPublishStatus("REJECTED");
        publish.setChangeLog(publish.getChangeLog() + "\n拒绝原因: " + reason);
        updateById(publish);

        log.info("任务发布审批拒绝: id={}, taskId={}, reason={}", id, publish.getTaskId(), reason);
    }

    /**
     * 获取任务的发布历史
     */
    public java.util.List<EtlTaskPublish> getHistory(Long taskId) {
        return lambdaQuery()
            .eq(EtlTaskPublish::getTaskId, taskId)
            .orderByDesc(EtlTaskPublish::getCreateTime)
            .list();
    }

    /**
     * 获取最新发布记录
     */
    public EtlTaskPublish getLatest(Long taskId) {
        return lambdaQuery()
            .eq(EtlTaskPublish::getTaskId, taskId)
            .orderByDesc(EtlTaskPublish::getCreateTime)
            .last("LIMIT 1")
            .one();
    }

    /**
     * 获取待发布任务列表
     */
    public java.util.List<EtlTaskPublish> getPendingList() {
        return lambdaQuery()
            .eq(EtlTaskPublish::getPublishStatus, "PENDING")
            .orderByDesc(EtlTaskPublish::getCreateTime)
            .list();
    }

    /**
     * 回滚到指定版本
     */
    @Transactional
    public Map<String, Object> rollback(Long publishId, String currentUser) {
        EtlTaskPublish publish = getById(publishId);
        if (publish == null) {
            throw new RuntimeException("发布记录不存在");
        }

        // 创建回滚记录
        EtlTaskPublish rollbackRecord = new EtlTaskPublish();
        rollbackRecord.setTaskId(publish.getTaskId());
        rollbackRecord.setTaskType(publish.getTaskType());
        rollbackRecord.setVersion(publish.getVersion());
        rollbackRecord.setPublishStatus("ROLLBACK");
        rollbackRecord.setPublishedBy(currentUser);
        rollbackRecord.setPublishedAt(LocalDateTime.now());
        rollbackRecord.setChangeLog("回滚到版本 v" + publish.getVersion());
        rollbackRecord.setSnapshotConfig(publish.getSnapshotConfig());
        rollbackRecord.setCreateTime(LocalDateTime.now());
        save(rollbackRecord);

        Map<String, Object> result = new HashMap<>();
        result.put("rollbackRecordId", rollbackRecord.getId());
        result.put("fromVersion", publish.getVersion());
        result.put("taskId", publish.getTaskId());
        result.put("status", "ROLLBACK");
        result.put("message", "已回滚到版本 v" + publish.getVersion());
        log.info("任务发布回滚: id={}, taskId={}, toVersion={}", publishId, publish.getTaskId(), publish.getVersion());
        return result;
    }

    /**
     * 对比两个发布版本的差异
     */
    public Map<String, Object> getDiff(Long taskId, Integer v1, Integer v2) {
        EtlTaskPublish publish1 = lambdaQuery()
            .eq(EtlTaskPublish::getTaskId, taskId)
            .eq(EtlTaskPublish::getVersion, v1)
            .one();
        EtlTaskPublish publish2 = lambdaQuery()
            .eq(EtlTaskPublish::getTaskId, taskId)
            .eq(EtlTaskPublish::getVersion, v2)
            .one();

        Map<String, Object> diff = new LinkedHashMap<>();
        diff.put("taskId", taskId);
        diff.put("version1", Map.of("version", v1, "changeLog", publish1 != null ? publish1.getChangeLog() : null, "publishedAt", publish1 != null ? publish1.getCreateTime() : null));
        diff.put("version2", Map.of("version", v2, "changeLog", publish2 != null ? publish2.getChangeLog() : null, "publishedAt", publish2 != null ? publish2.getCreateTime() : null));

        // 对比配置快照
        List<Map<String, Object>> changes = new ArrayList<>();
        if (publish1 != null && publish2 != null) {
            boolean configChanged = !Objects.equals(publish1.getSnapshotConfig(), publish2.getSnapshotConfig());
            boolean changeLogChanged = !Objects.equals(publish1.getChangeLog(), publish2.getChangeLog());
            if (configChanged) {
                changes.add(Map.of("field", "snapshotConfig", "changed", true, "description", "配置内容已变更"));
            }
            if (changeLogChanged) {
                changes.add(Map.of("field", "changeLog", "changed", true, "description", "变更说明已更新"));
            }
        } else if (publish1 == null && publish2 != null) {
            changes.add(Map.of("field", "all", "changed", true, "description", "新增版本"));
        } else if (publish1 != null && publish2 == null) {
            changes.add(Map.of("field", "all", "changed", true, "description", "删除版本"));
        }
        diff.put("changes", changes);
        diff.put("hasDiff", !changes.isEmpty());
        return diff;
    }
}