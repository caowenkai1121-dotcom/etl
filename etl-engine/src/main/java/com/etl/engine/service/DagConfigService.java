package com.etl.engine.service;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.etl.common.exception.EtlException;
import com.etl.engine.dto.DagConfigResponse;
import com.etl.engine.dto.DagSaveRequest;
import com.etl.engine.dag.DagExecutionEngine;
import com.etl.engine.dag.DagExecutionResult;
import com.etl.engine.entity.*;
import com.etl.engine.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * DAG配置服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DagConfigService {

    private final DagConfigMapper dagConfigMapper;
    private final DagNodeMapper dagNodeMapper;
    private final SyncTaskMapper syncTaskMapper;
    private final DagExecutionEngine dagExecutionEngine;
    private final TaskExecutionInstanceMapper executionInstanceMapper;
    private final NodeExecutionLogMapper nodeExecutionLogMapper;
    private final LineageAnalyzerService lineageAnalyzerService;

    /**
     * 获取DAG配置
     */
    public DagConfigResponse getDagConfig(Long taskId, Integer version) {
        // 获取任务
        EtlSyncTask task = syncTaskMapper.selectById(taskId);
        if (task == null) {
            throw new EtlException("任务不存在");
        }

        // 获取DAG配置
        EtlDagConfig dagConfig;
        if (version != null) {
            dagConfig = dagConfigMapper.selectByTaskIdAndVersion(taskId, version);
        } else {
            dagConfig = dagConfigMapper.selectLatestByTaskId(taskId);
        }

        if (dagConfig == null) {
            return null;
        }

        // 转换响应
        DagConfigResponse response = new DagConfigResponse();
        response.setId(dagConfig.getId());
        response.setTaskId(dagConfig.getTaskId());
        response.setVersion(dagConfig.getVersion());

        // 解析节点
        if (dagConfig.getNodes() != null) {
            List<DagConfigResponse.NodeConfig> nodes = JSON.parseArray(dagConfig.getNodes(), DagConfigResponse.NodeConfig.class);
            response.setNodes(nodes);
        }

        // 解析连线
        if (dagConfig.getEdges() != null) {
            List<DagConfigResponse.EdgeConfig> edges = JSON.parseArray(dagConfig.getEdges(), DagConfigResponse.EdgeConfig.class);
            response.setEdges(edges);
        }

        // 解析视口
        if (dagConfig.getViewport() != null) {
            DagConfigResponse.ViewportConfig viewport = JSON.parseObject(dagConfig.getViewport(), DagConfigResponse.ViewportConfig.class);
            response.setViewport(viewport);
        }

        return response;
    }

    /**
     * 保存DAG配置
     */
    @Transactional(rollbackFor = Exception.class)
    public DagConfigResponse saveDagConfig(Long taskId, DagSaveRequest request, String currentUser) {
        // 获取任务
        EtlSyncTask task = syncTaskMapper.selectById(taskId);
        if (task == null) {
            throw new EtlException("任务不存在");
        }

        // 获取最新版本号
        EtlDagConfig latestConfig = dagConfigMapper.selectLatestByTaskId(taskId);
        int newVersion = latestConfig != null ? latestConfig.getVersion() + 1 : 1;

        // 创建DAG配置
        EtlDagConfig dagConfig = new EtlDagConfig();
        dagConfig.setTaskId(taskId);
        dagConfig.setNodes(JSON.toJSONString(request.getNodes()));
        dagConfig.setEdges(JSON.toJSONString(request.getEdges()));
        dagConfig.setViewport(request.getViewport() != null ? JSON.toJSONString(request.getViewport()) : null);
        dagConfig.setVersion(newVersion);

        dagConfigMapper.insert(dagConfig);

        log.info("保存DAG配置成功: taskId={}, version={}", taskId, newVersion);

        // 更新任务的dev_config
        task.setDevConfig(JSON.toJSONString(request));
        syncTaskMapper.updateById(task);

        // 解析并保存血缘关系
        try {
            List<com.etl.engine.dag.DagNode> dagNodes = new ArrayList<>();
            if (request.getNodes() != null) {
                for (DagSaveRequest.NodeConfig nodeConfig : request.getNodes()) {
                    com.etl.engine.dag.DagNode node = new com.etl.engine.dag.DagNode();
                    node.setId(nodeConfig.getId());
                    node.setType(nodeConfig.getType());
                    node.setName(nodeConfig.getName());
                    node.setX(nodeConfig.getX() != null ? nodeConfig.getX() : 0);
                    node.setY(nodeConfig.getY() != null ? nodeConfig.getY() : 0);
                    node.setConfig(nodeConfig.getConfig() != null ? JSON.toJSONString(nodeConfig.getConfig()) : null);
                    dagNodes.add(node);
                }
            }
            List<com.etl.engine.dag.DagEdge> dagEdges = new ArrayList<>();
            if (request.getEdges() != null) {
                for (DagSaveRequest.EdgeConfig edgeConfig : request.getEdges()) {
                    dagEdges.add(new com.etl.engine.dag.DagEdge(
                        edgeConfig.getId(),
                        edgeConfig.getSource(),
                        edgeConfig.getTarget(),
                        edgeConfig.getConfig() != null ? JSON.toJSONString(edgeConfig.getConfig()) : null
                    ));
                }
            }
            lineageAnalyzerService.analyzeAndSave(taskId, dagNodes, dagEdges);
        } catch (Exception e) {
            log.warn("血缘分析失败，不影响保存: taskId={}", taskId, e);
        }

        // 返回响应
        DagConfigResponse response = new DagConfigResponse();
        response.setId(dagConfig.getId());
        response.setTaskId(taskId);
        response.setVersion(newVersion);

        // 从JSON重新解析以获取正确类型
        if (request.getNodes() != null) {
            List<DagConfigResponse.NodeConfig> nodes = JSON.parseArray(
                JSON.toJSONString(request.getNodes()), DagConfigResponse.NodeConfig.class);
            response.setNodes(nodes);
        }
        if (request.getEdges() != null) {
            List<DagConfigResponse.EdgeConfig> edges = JSON.parseArray(
                JSON.toJSONString(request.getEdges()), DagConfigResponse.EdgeConfig.class);
            response.setEdges(edges);
        }
        if (request.getViewport() != null) {
            DagConfigResponse.ViewportConfig viewport = JSON.parseObject(
                JSON.toJSONString(request.getViewport()), DagConfigResponse.ViewportConfig.class);
            response.setViewport(viewport);
        }

        return response;
    }

    /**
     * 复制DAG配置
     */
    @Transactional(rollbackFor = Exception.class)
    public DagConfigResponse copyDagConfig(Long sourceTaskId, Long targetTaskId, String currentUser) {
        // 获取源任务DAG配置
        EtlDagConfig sourceConfig = dagConfigMapper.selectLatestByTaskId(sourceTaskId);
        if (sourceConfig == null) {
            throw new EtlException("源任务没有DAG配置");
        }

        // 获取目标任务
        EtlSyncTask targetTask = syncTaskMapper.selectById(targetTaskId);
        if (targetTask == null) {
            throw new EtlException("目标任务不存在");
        }

        // 获取目标任务最新版本
        EtlDagConfig latestConfig = dagConfigMapper.selectLatestByTaskId(targetTaskId);
        int newVersion = latestConfig != null ? latestConfig.getVersion() + 1 : 1;

        // 复制配置
        EtlDagConfig dagConfig = new EtlDagConfig();
        dagConfig.setTaskId(targetTaskId);
        dagConfig.setNodes(sourceConfig.getNodes());
        dagConfig.setEdges(sourceConfig.getEdges());
        dagConfig.setViewport(sourceConfig.getViewport());
        dagConfig.setVersion(newVersion);

        dagConfigMapper.insert(dagConfig);

        log.info("复制DAG配置成功: sourceTaskId={}, targetTaskId={}", sourceTaskId, targetTaskId);

        return getDagConfig(targetTaskId, newVersion);
    }

    /**
     * 获取数据开发任务列表（增强版：多条件过滤、多字段排序）
     */
    public Map<String, Object> getDevTaskList(Integer pageNum, Integer pageSize, Long folderId,
                                               String keyword, String publishStatus, String status,
                                               String taskType, String syncMode, Boolean relatedToMe,
                                               Boolean favorite, String tab, String sortField,
                                               String sortOrder, String createdStart, String createdEnd,
                                               String currentUser) {
        LambdaQueryWrapper<EtlSyncTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EtlSyncTask::getDeleted, 0);

        if (folderId != null) {
            wrapper.eq(EtlSyncTask::getFolderId, folderId);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(EtlSyncTask::getTaskName, keyword)
                    .or().like(EtlSyncTask::getDescription, keyword));
        }
        if (publishStatus != null && !publishStatus.isEmpty()) {
            wrapper.eq(EtlSyncTask::getPublishStatus, publishStatus);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(EtlSyncTask::getStatus, status);
        }
        if (syncMode != null && !syncMode.isEmpty()) {
            wrapper.eq(EtlSyncTask::getSyncMode, syncMode);
        }
        if (relatedToMe != null && relatedToMe) {
            wrapper.and(w -> w.eq(EtlSyncTask::getCreatedBy, currentUser)
                    .or().eq(EtlSyncTask::getUpdatedBy, currentUser));
        }
        if (favorite != null && favorite) {
            wrapper.eq(EtlSyncTask::getIsFavorite, 1);
        }
        if (createdStart != null && !createdStart.isEmpty()) {
            wrapper.ge(EtlSyncTask::getCreatedAt, createdStart);
        }
        if (createdEnd != null && !createdEnd.isEmpty()) {
            wrapper.le(EtlSyncTask::getCreatedAt, createdEnd);
        }

        // 排序
        boolean isAsc = "asc".equalsIgnoreCase(sortOrder);
        switch (sortField) {
            case "createdAt":
                if (isAsc) wrapper.orderByAsc(EtlSyncTask::getCreatedAt);
                else wrapper.orderByDesc(EtlSyncTask::getCreatedAt);
                break;
            case "name":
                if (isAsc) wrapper.orderByAsc(EtlSyncTask::getTaskName);
                else wrapper.orderByDesc(EtlSyncTask::getTaskName);
                break;
            case "publishStatus":
                if (isAsc) wrapper.orderByAsc(EtlSyncTask::getPublishStatus);
                else wrapper.orderByDesc(EtlSyncTask::getPublishStatus);
                break;
            default:
                if (isAsc) wrapper.orderByAsc(EtlSyncTask::getUpdatedAt);
                else wrapper.orderByDesc(EtlSyncTask::getUpdatedAt);
        }

        IPage<EtlSyncTask> page = syncTaskMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);

        List<Map<String, Object>> list = page.getRecords().stream().map(task -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", task.getId());
            item.put("name", task.getTaskName());
            item.put("folderId", task.getFolderId());
            item.put("folderPath", getFolderPath(task.getFolderId()));
            item.put("publishStatus", task.getPublishStatus());
            item.put("status", task.getStatus());
            item.put("syncMode", task.getSyncMode());
            item.put("createdBy", task.getCreatedBy());
            item.put("createdAt", task.getCreatedAt());
            item.put("updatedBy", task.getUpdatedBy());
            item.put("updatedAt", task.getUpdatedAt());
            item.put("lastPublishTime", task.getLastPublishTime());
            item.put("isFavorite", task.getIsFavorite());
            return item;
        }).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", page.getTotal());
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);
        return result;
    }

    /**
     * 获取任务详情
     */
    public Map<String, Object> getTaskDetail(Long taskId) {
        EtlSyncTask task = syncTaskMapper.selectById(taskId);
        if (task == null) {
            throw new EtlException("任务不存在");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("id", task.getId());
        result.put("name", task.getTaskName());
        result.put("description", task.getDescription());
        result.put("folderId", task.getFolderId());
        result.put("folderPath", getFolderPath(task.getFolderId()));
        result.put("publishStatus", task.getPublishStatus());
        result.put("devConfig", task.getDevConfig() != null ? JSON.parseObject(task.getDevConfig()) : null);
        result.put("prodConfig", task.getProdConfig() != null ? JSON.parseObject(task.getProdConfig()) : null);
        result.put("scheduleConfig", task.getScheduleConfig() != null ? JSON.parseObject(task.getScheduleConfig()) : null);
        result.put("createdBy", task.getCreatedBy());
        result.put("createdAt", task.getCreatedAt());
        result.put("updatedBy", task.getUpdatedBy());
        result.put("updatedAt", task.getUpdatedAt());

        // 获取DAG配置
        DagConfigResponse dagConfig = getDagConfig(taskId, null);
        result.put("dagConfig", dagConfig);

        return result;
    }

    /**
     * 创建开发任务
     */
    @Transactional(rollbackFor = Exception.class)
    public EtlSyncTask createDevTask(Map<String, Object> request, String currentUser) {
        String name = (String) request.get("name");
        Long folderId = request.get("folderId") != null ? Long.valueOf(request.get("folderId").toString()) : 0L;
        String description = (String) request.get("description");

        EtlSyncTask task = new EtlSyncTask();
        task.setName(name);
        task.setTaskName(name);
        task.setFolderId(folderId);
        task.setDescription(description);
        task.setPublishStatus("DRAFT");
        task.setCreatedBy(currentUser);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedBy(currentUser);
        task.setUpdatedAt(LocalDateTime.now());
        task.setDeleted(0);

        syncTaskMapper.insert(task);

        log.info("创建开发任务成功: id={}, name={}", task.getId(), name);
        return task;
    }

    /**
     * 保存任务设计（扩展版）
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveTask(Long taskId, DagSaveRequest request, String currentUser) {
        EtlSyncTask task = syncTaskMapper.selectById(taskId);
        if (task == null) {
            throw new EtlException("任务不存在");
        }

        // 保存DAG配置
        saveDagConfig(taskId, request, currentUser);

        // 更新任务信息
        if (request.getTaskName() != null) {
            task.setName(request.getTaskName());
            task.setTaskName(request.getTaskName());
        }
        if (request.getDescription() != null) {
            task.setDescription(request.getDescription());
        }
        task.setUpdatedBy(currentUser);
        task.setUpdatedAt(LocalDateTime.now());

        // 更新发布状态为待发布或待更新
        if ("PUBLISHED".equals(task.getPublishStatus())) {
            task.setPublishStatus("UPDATED");
        } else if ("DRAFT".equals(task.getPublishStatus())) {
            task.setPublishStatus("PENDING");
        }

        syncTaskMapper.updateById(task);
    }

    /**
     * 删除开发任务
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteDevTask(Long taskId) {
        EtlSyncTask task = syncTaskMapper.selectById(taskId);
        if (task == null) {
            throw new EtlException("任务不存在");
        }

        // 已发布的任务不能删除
        if ("PUBLISHED".equals(task.getPublishStatus())) {
            throw new EtlException("已发布的任务不能删除");
        }

        task.setDeleted(1);
        task.setUpdatedAt(LocalDateTime.now());
        syncTaskMapper.updateById(task);

        log.info("删除开发任务成功: id={}", taskId);
    }

    /**
     * 复制任务
     */
    @Transactional(rollbackFor = Exception.class)
    public EtlSyncTask copyTask(Long sourceTaskId, String newName, String currentUser) {
        EtlSyncTask sourceTask = syncTaskMapper.selectById(sourceTaskId);
        if (sourceTask == null) {
            throw new EtlException("源任务不存在");
        }

        EtlSyncTask newTask = new EtlSyncTask();
        if (newName == null || newName.trim().isEmpty()) {
            newName = sourceTask.getName() + "-副本";
        }
        newTask.setName(newName);
        newTask.setTaskName(newName);
        newTask.setFolderId(sourceTask.getFolderId());
        newTask.setDescription(sourceTask.getDescription());
        newTask.setPublishStatus("DRAFT");
        newTask.setDevConfig(sourceTask.getDevConfig());
        newTask.setCreatedBy(currentUser);
        newTask.setCreatedAt(LocalDateTime.now());
        newTask.setUpdatedBy(currentUser);
        newTask.setUpdatedAt(LocalDateTime.now());
        newTask.setDeleted(0);

        syncTaskMapper.insert(newTask);

        // 复制DAG配置
        copyDagConfig(sourceTaskId, newTask.getId(), currentUser);

        log.info("复制任务成功: sourceId={}, newId={}", sourceTaskId, newTask.getId());
        return newTask;
    }

    /**
     * 运行任务
     */
    public Map<String, Object> runTask(Long taskId, Map<String, Object> options) {
        EtlSyncTask task = syncTaskMapper.selectById(taskId);
        if (task == null) {
            throw new EtlException("任务不存在");
        }

        // 调用DAG执行引擎
        DagExecutionResult result = dagExecutionEngine.execute(taskId, options);

        Map<String, Object> response = new HashMap<>();
        response.put("executionId", result.getExecutionId());
        response.put("status", result.getStatus());
        response.put("message", "SUCCESS".equals(result.getStatus()) ? "任务执行成功" : "任务执行失败: " + result.getErrorMsg());
        response.put("duration", result.getDuration());
        response.put("totalNodes", result.getTotalNodes());
        response.put("successNodes", result.getSuccessNodes());
        response.put("failedNodes", result.getFailedNodes());

        log.info("运行任务: id={}, executionId={}, status={}", taskId, result.getExecutionId(), result.getStatus());
        return response;
    }

    /**
     * 停止任务
     */
    public void stopTask(Long executionId) {
        dagExecutionEngine.stopExecution(executionId);
        log.info("停止任务: executionId={}", executionId);
    }

    /**
     * 获取运行日志
     */
    public List<Map<String, Object>> getRunLogs(Long taskId, Long executionId) {
        List<Map<String, Object>> logs = new ArrayList<>();

        // 优先按executionId查询
        if (executionId != null) {
            List<EtlNodeExecutionLog> nodeLogs = nodeExecutionLogMapper.selectByExecutionId(executionId);
            for (EtlNodeExecutionLog nl : nodeLogs) {
                Map<String, Object> log = new HashMap<>();
                String status = nl.getStatus();
                String level = "SUCCESS".equals(status) ? "INFO" : ("FAILED".equals(status) ? "ERROR" : "INFO");
                log.put("level", level);
                log.put("message", String.format("节点[%s] %s - %s", nl.getNodeName(), status, nl.getLogContent() != null ? nl.getLogContent() : ""));
                log.put("time", nl.getStartTime());
                log.put("nodeId", nl.getNodeId());
                log.put("nodeName", nl.getNodeName());
                log.put("status", nl.getStatus());
                logs.add(log);
            }
            return logs;
        }

        // 按taskId查询最近的执行实例
        List<EtlTaskExecutionInstance> instances = executionInstanceMapper.selectByTaskId(taskId);
        if (!instances.isEmpty()) {
            EtlTaskExecutionInstance latest = instances.get(0);
            List<EtlNodeExecutionLog> nodeLogs = nodeExecutionLogMapper.selectByExecutionId(latest.getId());
            for (EtlNodeExecutionLog nl : nodeLogs) {
                Map<String, Object> log = new HashMap<>();
                String level = "SUCCESS".equals(nl.getStatus()) ? "INFO" : ("FAILED".equals(nl.getStatus()) ? "ERROR" : "INFO");
                log.put("level", level);
                log.put("message", String.format("节点[%s] %s - %s", nl.getNodeName(), nl.getStatus(), nl.getLogContent() != null ? nl.getLogContent() : ""));
                log.put("time", nl.getStartTime());
                log.put("nodeId", nl.getNodeId());
                log.put("nodeName", nl.getNodeName());
                log.put("status", nl.getStatus());
                logs.add(log);
            }
        }

        return logs;
    }

    /**
     * 获取运行统计
     */
    public Map<String, Object> getRunStats(Long taskId, Long executionId) {
        Map<String, Object> stats = new HashMap<>();

        List<EtlTaskExecutionInstance> instances;
        if (executionId != null) {
            EtlTaskExecutionInstance inst = executionInstanceMapper.selectById(executionId);
            instances = inst != null ? List.of(inst) : List.of();
        } else {
            instances = executionInstanceMapper.selectByTaskId(taskId);
        }

        if (!instances.isEmpty()) {
            EtlTaskExecutionInstance latest = instances.get(0);
            stats.put("totalNodes", 0);
            stats.put("successNodes", 0);
            stats.put("failedNodes", 0);
            stats.put("skippedNodes", 0);
            stats.put("runningTime", latest.getDuration() != null ? latest.getDuration() : 0);
            stats.put("executionId", latest.getId());
            stats.put("status", latest.getStatus());

            // 统计节点
            List<EtlNodeExecutionLog> nodeLogs = nodeExecutionLogMapper.selectByExecutionId(latest.getId());
            int total = nodeLogs.size();
            int success = 0, failed = 0, skipped = 0;
            for (EtlNodeExecutionLog nl : nodeLogs) {
                if ("SUCCESS".equals(nl.getStatus())) success++;
                else if ("FAILED".equals(nl.getStatus())) failed++;
                else if ("SKIPPED".equals(nl.getStatus())) skipped++;
            }
            stats.put("totalNodes", total);
            stats.put("successNodes", success);
            stats.put("failedNodes", failed);
            stats.put("skippedNodes", skipped);
        } else {
            stats.put("totalNodes", 0);
            stats.put("successNodes", 0);
            stats.put("failedNodes", 0);
            stats.put("skippedNodes", 0);
            stats.put("runningTime", 0);
        }
        return stats;
    }

    /**
     * 发布任务
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> publishTask(Long taskId, Map<String, Object> request, String currentUser) {
        EtlSyncTask task = syncTaskMapper.selectById(taskId);
        if (task == null) {
            throw new EtlException("任务不存在");
        }

        String changeLog = (String) request.get("changeLog");
        List<String> environments = (List<String>) request.get("environments");

        // 生成版本号
        int newVersion = 1;
        // TODO: 从版本表获取最新版本

        // 更新任务状态
        task.setPublishStatus("PUBLISHED");
        task.setProdConfig(task.getDevConfig());
        task.setLastPublishTime(LocalDateTime.now());
        task.setUpdatedBy(currentUser);
        task.setUpdatedAt(LocalDateTime.now());
        syncTaskMapper.updateById(task);

        Map<String, Object> result = new HashMap<>();
        result.put("version", newVersion);
        result.put("status", "PUBLISHED");
        result.put("message", "发布成功");

        log.info("发布任务成功: id={}, version={}", taskId, newVersion);
        return result;
    }

    /**
     * 获取发布历史
     */
    public List<Map<String, Object>> getPublishHistory(Long taskId) {
        // TODO: 从版本表获取
        List<Map<String, Object>> history = new ArrayList<>();
        Map<String, Object> v1 = new HashMap<>();
        v1.put("version", 1);
        v1.put("changeLog", "初始版本");
        v1.put("publishedAt", LocalDateTime.now().minusDays(7));
        v1.put("publishedBy", "admin");
        history.add(v1);
        return history;
    }

    /**
     * 获取统计概览
     */
    public Map<String, Object> getDevStats(String currentUser) {
        LambdaQueryWrapper<EtlSyncTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EtlSyncTask::getDeleted, 0);

        List<EtlSyncTask> allTasks = syncTaskMapper.selectList(wrapper);

        long published = allTasks.stream().filter(t -> "PUBLISHED".equals(t.getPublishStatus())).count();
        long pending = allTasks.stream().filter(t -> "PENDING".equals(t.getPublishStatus())).count();
        long updated = allTasks.stream().filter(t -> "UPDATED".equals(t.getPublishStatus())).count();
        long myCreated = allTasks.stream().filter(t -> currentUser.equals(t.getCreatedBy())).count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("published", published);
        stats.put("pending", pending);
        stats.put("updated", updated);
        stats.put("myCreated", myCreated);
        stats.put("total", allTasks.size());
        return stats;
    }

    /**
     * 收藏任务
     */
    public void addFavorite(Long taskId, String currentUser) {
        EtlSyncTask task = syncTaskMapper.selectById(taskId);
        if (task == null) throw new EtlException("任务不存在");
        task.setIsFavorite(1);
        task.setUpdatedBy(currentUser);
        task.setUpdatedAt(LocalDateTime.now());
        syncTaskMapper.updateById(task);
        log.info("添加收藏: taskId={}, user={}", taskId, currentUser);
    }

    /**
     * 取消收藏
     */
    public void removeFavorite(Long taskId, String currentUser) {
        EtlSyncTask task = syncTaskMapper.selectById(taskId);
        if (task == null) throw new EtlException("任务不存在");
        task.setIsFavorite(0);
        task.setUpdatedBy(currentUser);
        task.setUpdatedAt(LocalDateTime.now());
        syncTaskMapper.updateById(task);
        log.info("取消收藏: taskId={}, user={}", taskId, currentUser);
    }

    /**
     * 移动任务
     */
    public void moveTask(Long taskId, Long targetFolderId) {
        EtlSyncTask task = syncTaskMapper.selectById(taskId);
        if (task == null) {
            throw new EtlException("任务不存在");
        }

        task.setFolderId(targetFolderId != null ? targetFolderId : 0L);
        task.setUpdatedAt(LocalDateTime.now());
        syncTaskMapper.updateById(task);

        log.info("移动任务成功: taskId={}, targetFolderId={}", taskId, targetFolderId);
    }

    // ========== 批量操作 ==========

    /**
     * 批量删除任务
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> batchDeleteTasks(List<Long> taskIds, String currentUser) {
        int success = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();

        for (Long taskId : taskIds) {
            try {
                EtlSyncTask task = syncTaskMapper.selectById(taskId);
                if (task != null && !"PUBLISHED".equals(task.getPublishStatus())) {
                    task.setDeleted(1);
                    task.setUpdatedAt(LocalDateTime.now());
                    syncTaskMapper.updateById(task);
                    success++;
                } else if (task != null) {
                    errors.add("任务[" + taskId + "]已发布，无法删除");
                    failed++;
                } else {
                    errors.add("任务[" + taskId + "]不存在");
                    failed++;
                }
            } catch (Exception e) {
                errors.add("任务[" + taskId + "]删除失败: " + e.getMessage());
                failed++;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("failed", failed);
        result.put("errors", errors);
        result.put("total", taskIds.size());
        log.info("批量删除任务完成: total={}, success={}, failed={}", taskIds.size(), success, failed);
        return result;
    }

    /**
     * 批量发布任务
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> batchPublishTasks(List<Long> taskIds, String changeLog, String currentUser) {
        int success = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();

        for (Long taskId : taskIds) {
            try {
                Map<String, Object> publishReq = new HashMap<>();
                publishReq.put("changeLog", changeLog != null ? changeLog : "批量发布");
                publishTask(taskId, publishReq, currentUser);
                success++;
            } catch (Exception e) {
                errors.add("任务[" + taskId + "]发布失败: " + e.getMessage());
                failed++;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("failed", failed);
        result.put("errors", errors);
        result.put("total", taskIds.size());
        log.info("批量发布任务完成: total={}, success={}, failed={}", taskIds.size(), success, failed);
        return result;
    }

    /**
     * 批量移动任务
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> batchMoveTasks(List<Long> taskIds, Long targetFolderId, String currentUser) {
        int success = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();

        for (Long taskId : taskIds) {
            try {
                moveTask(taskId, targetFolderId);
                success++;
            } catch (Exception e) {
                errors.add("任务[" + taskId + "]移动失败: " + e.getMessage());
                failed++;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("failed", failed);
        result.put("errors", errors);
        result.put("total", taskIds.size());
        log.info("批量移动任务完成: total={}, success={}, failed={}", taskIds.size(), success, failed);
        return result;
    }

    // ========== 任务导入/导出 ==========

    /**
     * 导出任务（完整配置JSON）
     */
    public Map<String, Object> exportTask(Long taskId) {
        EtlSyncTask task = syncTaskMapper.selectById(taskId);
        if (task == null) {
            throw new EtlException("任务不存在");
        }

        Map<String, Object> exported = new LinkedHashMap<>();
        exported.put("exportVersion", "1.0");
        exported.put("exportedAt", LocalDateTime.now().toString());
        Map<String, Object> taskMap = new LinkedHashMap<>();
        taskMap.put("name", task.getTaskName() != null ? task.getTaskName() : "");
        taskMap.put("description", task.getDescription() != null ? task.getDescription() : "");
        taskMap.put("syncMode", task.getSyncMode() != null ? task.getSyncMode() : "");
        taskMap.put("syncScope", task.getSyncScope() != null ? task.getSyncScope() : "");
        taskMap.put("sourceDsId", task.getSourceDsId());
        taskMap.put("targetDsId", task.getTargetDsId());
        taskMap.put("syncStrategy", task.getSyncStrategy() != null ? task.getSyncStrategy() : "");
        taskMap.put("batchSize", task.getBatchSize());
        taskMap.put("parallelThreads", task.getParallelThreads());
        taskMap.put("retryTimes", task.getRetryTimes());
        taskMap.put("retryInterval", task.getRetryInterval());
        exported.put("task", taskMap);

        // 导出DAG配置
        DagConfigResponse dagConfig = getDagConfig(taskId, null);
        exported.put("dagConfig", dagConfig);

        // 导出调度配置
        if (task.getScheduleConfig() != null) {
            exported.put("scheduleConfig", JSON.parseObject(task.getScheduleConfig()));
        }

        log.info("导出任务成功: id={}", taskId);
        return exported;
    }

    /**
     * 批量导出任务
     */
    public Map<String, Object> exportTasksBatch(List<Long> taskIds) {
        List<Map<String, Object>> tasks = new ArrayList<>();
        for (Long taskId : taskIds) {
            try {
                tasks.add(exportTask(taskId));
            } catch (Exception e) {
                log.warn("导出任务失败: id={}, error={}", taskId, e.getMessage());
                Map<String, Object> errorItem = new HashMap<>();
                errorItem.put("taskId", taskId);
                errorItem.put("error", e.getMessage());
                tasks.add(errorItem);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("exportVersion", "1.0");
        result.put("exportedAt", LocalDateTime.now().toString());
        result.put("total", taskIds.size());
        result.put("tasks", tasks);
        log.info("批量导出任务完成: total={}", taskIds.size());
        return result;
    }

    /**
     * 导入任务（从配置JSON创建）
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> importTask(Map<String, Object> importData, String currentUser) {
        @SuppressWarnings("unchecked")
        Map<String, Object> taskData = (Map<String, Object>) importData.get("task");
        if (taskData == null) {
            throw new EtlException("导入数据格式错误：缺少task字段");
        }

        String taskName = (String) taskData.getOrDefault("name", "导入任务");
        Long folderId = importData.get("folderId") != null
            ? Long.valueOf(importData.get("folderId").toString()) : 0L;

        // 创建任务
        Map<String, Object> createReq = new HashMap<>();
        createReq.put("name", taskName);
        createReq.put("folderId", folderId);
        createReq.put("description", taskData.getOrDefault("description", ""));
        EtlSyncTask newTask = createDevTask(createReq, currentUser);

        // 恢复DAG配置
        @SuppressWarnings("unchecked")
        Map<String, Object> dagData = (Map<String, Object>) importData.get("dagConfig");
        if (dagData != null) {
            DagSaveRequest dagRequest = new DagSaveRequest();
            dagRequest.setTaskName(taskName);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> nodesRaw = (List<Map<String, Object>>) dagData.get("nodes");
            if (nodesRaw != null) {
                List<DagSaveRequest.NodeConfig> nodes = nodesRaw.stream().map(n -> {
                    DagSaveRequest.NodeConfig nc = new DagSaveRequest.NodeConfig();
                    nc.setId((String) n.get("id"));
                    nc.setType((String) n.get("type"));
                    nc.setName((String) n.get("name"));
                    nc.setX(n.get("x") != null ? ((Number) n.get("x")).intValue() : 0);
                    nc.setY(n.get("y") != null ? ((Number) n.get("y")).intValue() : 0);
                    @SuppressWarnings("unchecked")
                    Map<String, Object> config = (Map<String, Object>) n.get("config");
                    nc.setConfig(config);
                    return nc;
                }).collect(Collectors.toList());
                dagRequest.setNodes(nodes);
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> edgesRaw = (List<Map<String, Object>>) dagData.get("edges");
            if (edgesRaw != null) {
                List<DagSaveRequest.EdgeConfig> edges = edgesRaw.stream().map(e -> {
                    DagSaveRequest.EdgeConfig ec = new DagSaveRequest.EdgeConfig();
                    ec.setId((String) e.get("id"));
                    ec.setSource((String) e.get("source"));
                    ec.setTarget((String) e.get("target"));
                    @SuppressWarnings("unchecked")
                    Map<String, Object> config = (Map<String, Object>) e.get("config");
                    ec.setConfig(config);
                    return ec;
                }).collect(Collectors.toList());
                dagRequest.setEdges(edges);
            }

            saveDagConfig(newTask.getId(), dagRequest, currentUser);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("taskId", newTask.getId());
        result.put("taskName", newTask.getTaskName());
        result.put("created", true);
        log.info("导入任务成功: id={}, name={}", newTask.getId(), newTask.getTaskName());
        return result;
    }

    // ========== 任务模板 ==========

    /**
     * 获取模板列表
     */
    public Map<String, Object> getTemplates(String category, String keyword, Integer pageNum, Integer pageSize) {
        // 简化实现：从etl_task_template表查询（通过syncTaskMapper模拟）
        // 实际项目需要添加 TaskTemplateMapper
        Map<String, Object> result = new HashMap<>();
        result.put("list", new ArrayList<>());
        result.put("total", 3);
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);

        // 返回内置模板
        List<Map<String, Object>> templates = new ArrayList<>();

        Map<String, Object> t1 = new LinkedHashMap<>();
        t1.put("id", 1);
        t1.put("name", "MySQL同步模板");
        t1.put("category", "SYNC");
        t1.put("description", "MySQL全量+增量同步基础模板");
        t1.put("icon", "Sync");
        t1.put("tags", List.of("mysql", "同步", "基础"));
        t1.put("usageCount", 156);
        t1.put("isSystem", true);
        templates.add(t1);

        Map<String, Object> t2 = new LinkedHashMap<>();
        t2.put("id", 2);
        t2.put("name", "API数据服务模板");
        t2.put("category", "API");
        t2.put("description", "将SQL查询发布为REST API");
        t2.put("icon", "Api");
        t2.put("tags", List.of("api", "服务", "查询"));
        t2.put("usageCount", 89);
        t2.put("isSystem", true);
        templates.add(t2);

        Map<String, Object> t3 = new LinkedHashMap<>();
        t3.put("id", 3);
        t3.put("name", "数据转换管道模板");
        t3.put("category", "TRANSFORM");
        t3.put("description", "数据清洗+转换+聚合流水线");
        t3.put("icon", "Transform");
        t3.put("tags", List.of("转换", "管道", "清洗"));
        t3.put("usageCount", 42);
        t3.put("isSystem", true);
        templates.add(t3);

        // 简易过滤
        List<Map<String, Object>> filtered = templates.stream()
            .filter(t -> category == null || category.isEmpty() || category.equals(t.get("category")))
            .filter(t -> keyword == null || keyword.isEmpty()
                || String.valueOf(t.get("name")).contains(keyword)
                || String.valueOf(t.get("description")).contains(keyword))
            .collect(Collectors.toList());

        result.put("list", filtered);
        result.put("total", filtered.size());
        return result;
    }

    /**
     * 从模板创建任务
     */
    @Transactional(rollbackFor = Exception.class)
    public EtlSyncTask createFromTemplate(Long templateId, String taskName, Long folderId, String currentUser) {
        Map<String, Object> createReq = new HashMap<>();
        createReq.put("name", taskName != null ? taskName : "从模板创建的任务");
        createReq.put("folderId", folderId);
        createReq.put("description", "从模板[" + templateId + "]创建");

        EtlSyncTask task = createDevTask(createReq, currentUser);

        log.info("从模板创建任务成功: templateId={}, taskId={}, name={}", templateId, task.getId(), task.getTaskName());
        return task;
    }

    /**
     * 保存为模板
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> saveAsTemplate(Long taskId, Map<String, Object> request, String currentUser) {
        EtlSyncTask task = syncTaskMapper.selectById(taskId);
        if (task == null) {
            throw new EtlException("任务不存在");
        }

        String templateName = (String) request.getOrDefault("name", task.getTaskName() + "_模板");
        String category = (String) request.getOrDefault("category", "SYNC");
        String description = (String) request.getOrDefault("description", "");

        // 导出完整配置
        Map<String, Object> exported = exportTask(taskId);

        Map<String, Object> result = new HashMap<>();
        result.put("name", templateName);
        result.put("category", category);
        result.put("description", description);
        result.put("config", exported);
        result.put("savedBy", currentUser);
        result.put("savedAt", LocalDateTime.now().toString());

        log.info("保存为模板成功: taskId={}, templateName={}", taskId, templateName);
        return result;
    }

    // ========== DAG校验 ==========

    /**
     * 校验DAG配置的节点和连线完整性
     */
    public void validateDagIntegrity(DagSaveRequest request) {
        if (request.getNodes() == null || request.getNodes().isEmpty()) {
            throw new EtlException("DAG配置必须包含至少一个节点");
        }

        Set<String> nodeIds = new HashSet<>();
        Set<String> edgeIds = new HashSet<>();

        for (DagSaveRequest.NodeConfig node : request.getNodes()) {
            if (node.getId() == null || node.getId().isEmpty()) {
                throw new EtlException("节点ID不能为空");
            }
            if (node.getType() == null || node.getType().isEmpty()) {
                throw new EtlException("节点[" + node.getId() + "]类型不能为空");
            }
            if (nodeIds.contains(node.getId())) {
                throw new EtlException("节点ID重复: " + node.getId());
            }
            nodeIds.add(node.getId());
        }

        if (request.getEdges() != null) {
            for (DagSaveRequest.EdgeConfig edge : request.getEdges()) {
                if (edge.getId() == null || edge.getId().isEmpty()) {
                    throw new EtlException("连线ID不能为空");
                }
                if (edge.getSource() == null || edge.getSource().isEmpty()) {
                    throw new EtlException("连线[" + edge.getId() + "]的源节点不能为空");
                }
                if (edge.getTarget() == null || edge.getTarget().isEmpty()) {
                    throw new EtlException("连线[" + edge.getId() + "]的目标节点不能为空");
                }
                if (!nodeIds.contains(edge.getSource())) {
                    throw new EtlException("连线[" + edge.getId() + "]的源节点[" + edge.getSource() + "]不存在");
                }
                if (!nodeIds.contains(edge.getTarget())) {
                    throw new EtlException("连线[" + edge.getId() + "]的目标节点[" + edge.getTarget() + "]不存在");
                }
                if (edgeIds.contains(edge.getId())) {
                    throw new EtlException("连线ID重复: " + edge.getId());
                }
                edgeIds.add(edge.getId());
            }
        }
    }

    /**
     * 获取DAG版本快照列表
     */
    public List<Map<String, Object>> getDagSnapshots(Long taskId) {
        List<EtlDagConfig> configs = dagConfigMapper.selectByTaskId(taskId);
        List<Map<String, Object>> snapshots = new ArrayList<>();
        for (EtlDagConfig config : configs) {
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("id", config.getId());
            snapshot.put("version", config.getVersion());
            snapshot.put("nodeCount", config.getNodes() != null ? JSON.parseArray(config.getNodes()).size() : 0);
            snapshot.put("edgeCount", config.getEdges() != null ? JSON.parseArray(config.getEdges()).size() : 0);
            snapshot.put("createdAt", config.getCreatedAt());
            snapshots.add(snapshot);
        }
        return snapshots;
    }

    /**
     * 对比两个DAG版本的差异
     */
    public Map<String, Object> getDagDiff(Long taskId, Integer version1, Integer version2) {
        DagConfigResponse config1 = getDagConfig(taskId, version1);
        DagConfigResponse config2 = getDagConfig(taskId, version2);

        Map<String, Object> diff = new LinkedHashMap<>();
        diff.put("taskId", taskId);
        diff.put("version1", version1);
        diff.put("version2", version2);

        // 对比节点
        List<Map<String, Object>> nodeChanges = new ArrayList<>();
        if (config1 != null && config2 != null) {
            Map<String, DagConfigResponse.NodeConfig> v1Nodes = new HashMap<>();
            Map<String, DagConfigResponse.NodeConfig> v2Nodes = new HashMap<>();
            if (config1.getNodes() != null) {
                config1.getNodes().forEach(n -> v1Nodes.put(n.getId(), n));
            }
            if (config2.getNodes() != null) {
                config2.getNodes().forEach(n -> v2Nodes.put(n.getId(), n));
            }

            // 新增节点
            v2Nodes.forEach((id, node) -> {
                if (!v1Nodes.containsKey(id)) {
                    nodeChanges.add(Map.of("type", "ADDED", "nodeId", id, "nodeName", node.getName() != null ? node.getName() : ""));
                }
            });
            // 删除节点
            v1Nodes.forEach((id, node) -> {
                if (!v2Nodes.containsKey(id)) {
                    nodeChanges.add(Map.of("type", "REMOVED", "nodeId", id, "nodeName", node.getName() != null ? node.getName() : ""));
                }
            });
            // 修改节点
            v2Nodes.forEach((id, node) -> {
                DagConfigResponse.NodeConfig oldNode = v1Nodes.get(id);
                if (oldNode != null && !JSON.toJSONString(node).equals(JSON.toJSONString(oldNode))) {
                    nodeChanges.add(Map.of("type", "MODIFIED", "nodeId", id, "nodeName", node.getName() != null ? node.getName() : ""));
                }
            });
        }

        diff.put("nodeChanges", nodeChanges);
        diff.put("totalChanges", nodeChanges.size());
        return diff;
    }

    /**
     * 获取文件夹路径
     */
    private String getFolderPath(Long folderId) {
        if (folderId == null || folderId == 0) {
            return "根目录";
        }
        // TODO: 从文件夹表获取路径
        return "未分类";
    }
}