package com.etl.engine.dag;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.etl.engine.entity.EtlDagConfig;
import com.etl.engine.entity.EtlDagNode;
import com.etl.engine.entity.EtlNodeExecutionLog;
import com.etl.engine.entity.EtlTaskExecutionInstance;
import com.etl.engine.event.DagExecutionEvent;
import com.etl.engine.mapper.DagConfigMapper;
import com.etl.engine.mapper.NodeExecutionLogMapper;
import com.etl.engine.mapper.TaskExecutionInstanceMapper;
import com.etl.engine.service.NodeExecutorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

/**
 * DAG执行引擎
 * 支持拓扑排序、并行执行、条件分支、循环容器、子任务调用
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DagExecutionEngine {

    private final NodeExecutorService nodeExecutor;
    private final DagConfigMapper dagConfigMapper;
    private final TaskExecutionInstanceMapper executionInstanceMapper;
    private final NodeExecutionLogMapper nodeExecutionLogMapper;
    private final ApplicationEventPublisher eventPublisher;

    // 执行线程池
    private final ExecutorService executorService = Executors.newFixedThreadPool(8);

    // 活跃的执行任务
    private final Map<Long, Future<?>> activeExecutions = new ConcurrentHashMap<>();

    /**
     * 执行DAG任务
     */
    public DagExecutionResult execute(Long taskId, Map<String, Object> runParams) {
        Long executionId = System.currentTimeMillis();
        log.info("[DagEngine] 开始执行DAG: taskId={}, executionId={}", taskId, executionId);

        DagExecutionContext context = new DagExecutionContext(executionId, taskId);
        if (runParams != null) {
            context.setRunParams(runParams);
            context.setDebugMode(Boolean.TRUE.equals(runParams.get("debugMode")) || "DEBUG".equals(runParams.get("runMode")));
        }

        // 创建执行实例记录
        EtlTaskExecutionInstance instance = new EtlTaskExecutionInstance();
        instance.setTaskId(taskId);
        instance.setStatus("RUNNING");
        instance.setTriggerType(runParams != null ? "MANUAL" : "SCHEDULE");
        instance.setStartTime(LocalDateTime.now());
        executionInstanceMapper.insert(instance);
        Long dbExecutionId = instance.getId();

        try {
            // 发布任务开始事件
            publishEvent(DagExecutionEvent.EventType.TASK_START, taskId, dbExecutionId, null, null, "RUNNING", "任务开始执行");

            // 1. 加载DAG配置
            EtlDagConfig dagConfig = dagConfigMapper.selectLatestByTaskId(taskId);
            if (dagConfig == null) {
                throw new RuntimeException("DAG配置不存在: taskId=" + taskId);
            }

            // 2. 解析节点和边
            List<DagNode> nodes = parseNodes(dagConfig.getNodes());
            List<DagEdge> edges = parseEdges(dagConfig.getEdges());

            // 3. 拓扑排序
            List<String> topoOrder = topologicalSort(nodes, edges);
            log.info("[DagEngine] 拓扑排序结果: {}", topoOrder);

            // 4. 构建邻接表和入度表
            Map<String, List<DagEdge>> adjacency = buildAdjacency(edges);
            Map<String, Integer> inDegree = buildInDegree(nodes, edges);

            // 5. 执行DAG
            executeDag(nodes, edges, topoOrder, adjacency, inDegree, context, dbExecutionId);

            context.setEndTime(LocalDateTime.now());

            // 6. 构建结果
            DagExecutionResult result = buildResult(context, nodes);
            result.setExecutionId(dbExecutionId);

            // 更新执行实例状态
            instance.setStatus(result.getStatus());
            instance.setEndTime(LocalDateTime.now());
            instance.setDuration(context.getDurationSeconds());
            if (result.getErrorMsg() != null) {
                instance.setErrorMsg(result.getErrorMsg());
            }
            executionInstanceMapper.updateById(instance);

            // 发布任务完成事件
            publishEvent(DagExecutionEvent.EventType.TASK_COMPLETE, taskId, dbExecutionId, null, null, result.getStatus(), "任务执行完成，状态: " + result.getStatus());

            log.info("[DagEngine] DAG执行完成: taskId={}, status={}, duration={}s",
                taskId, result.getStatus(), context.getDurationSeconds());
            return result;

        } catch (Exception e) {
            log.error("[DagEngine] DAG执行异常: taskId={}", taskId, e);
            context.setEndTime(LocalDateTime.now());

            // 发布任务失败事件
            publishEvent(DagExecutionEvent.EventType.TASK_FAILED, taskId, dbExecutionId, null, null, "FAILED", e.getMessage());

            instance.setStatus("FAILED");
            instance.setEndTime(LocalDateTime.now());
            instance.setDuration(context.getDurationSeconds());
            instance.setErrorMsg(e.getMessage());
            executionInstanceMapper.updateById(instance);

            DagExecutionResult result = new DagExecutionResult();
            result.setExecutionId(dbExecutionId);
            result.setTaskId(taskId);
            result.setStatus("FAILED");
            result.setErrorMsg(e.getMessage());
            result.setDuration(context.getDurationSeconds());
            return result;
        }
    }

    /**
     * 停止执行
     */
    public void stopExecution(Long executionId) {
        Future<?> future = activeExecutions.get(executionId);
        if (future != null) {
            future.cancel(true);
            activeExecutions.remove(executionId);
            log.info("[DagEngine] 停止执行: executionId={}", executionId);
        }
    }

    /**
     * 解析节点
     */
    private List<DagNode> parseNodes(String nodesJson) {
        List<DagNode> nodes = new ArrayList<>();
        if (nodesJson == null || nodesJson.isEmpty()) return nodes;
        JSONArray array = JSON.parseArray(nodesJson);
        for (int i = 0; i < array.size(); i++) {
            JSONObject obj = array.getJSONObject(i);
            DagNode node = new DagNode();
            node.setId(obj.getString("id"));
            node.setType(obj.getString("type"));
            node.setName(obj.getString("name"));
            node.setX(obj.getIntValue("x"));
            node.setY(obj.getIntValue("y"));
            node.setConfig(obj.getString("config"));
            nodes.add(node);
        }
        return nodes;
    }

    /**
     * 解析边
     */
    private List<DagEdge> parseEdges(String edgesJson) {
        List<DagEdge> edges = new ArrayList<>();
        if (edgesJson == null || edgesJson.isEmpty()) return edges;
        JSONArray array = JSON.parseArray(edgesJson);
        for (int i = 0; i < array.size(); i++) {
            JSONObject obj = array.getJSONObject(i);
            DagEdge edge = new DagEdge(
                obj.getString("id"),
                obj.getString("source"),
                obj.getString("target"),
                obj.getString("condition")
            );
            edges.add(edge);
        }
        return edges;
    }

    /**
     * 拓扑排序（Kahn算法）
     */
    private List<String> topologicalSort(List<DagNode> nodes, List<DagEdge> edges) {
        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, List<String>> adjacency = new HashMap<>();

        for (DagNode node : nodes) {
            inDegree.put(node.getId(), 0);
            adjacency.put(node.getId(), new ArrayList<>());
        }

        for (DagEdge edge : edges) {
            if (adjacency.containsKey(edge.getSource()) && inDegree.containsKey(edge.getTarget())) {
                adjacency.get(edge.getSource()).add(edge.getTarget());
                inDegree.put(edge.getTarget(), inDegree.get(edge.getTarget()) + 1);
            }
        }

        Queue<String> queue = new LinkedList<>();
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.offer(entry.getKey());
            }
        }

        List<String> result = new ArrayList<>();
        while (!queue.isEmpty()) {
            String nodeId = queue.poll();
            result.add(nodeId);
            for (String neighbor : adjacency.get(nodeId)) {
                int newDegree = inDegree.get(neighbor) - 1;
                inDegree.put(neighbor, newDegree);
                if (newDegree == 0) {
                    queue.offer(neighbor);
                }
            }
        }

        if (result.size() != nodes.size()) {
            log.warn("[DagEngine] DAG存在环，拓扑排序不完整");
        }

        return result;
    }

    /**
     * 构建邻接表
     */
    private Map<String, List<DagEdge>> buildAdjacency(List<DagEdge> edges) {
        Map<String, List<DagEdge>> adjacency = new HashMap<>();
        for (DagEdge edge : edges) {
            adjacency.computeIfAbsent(edge.getSource(), k -> new ArrayList<>()).add(edge);
        }
        return adjacency;
    }

    /**
     * 构建入度表
     */
    private Map<String, Integer> buildInDegree(List<DagNode> nodes, List<DagEdge> edges) {
        Map<String, Integer> inDegree = new HashMap<>();
        for (DagNode node : nodes) {
            inDegree.put(node.getId(), 0);
        }
        for (DagEdge edge : edges) {
            if (inDegree.containsKey(edge.getTarget())) {
                inDegree.put(edge.getTarget(), inDegree.get(edge.getTarget()) + 1);
            }
        }
        return inDegree;
    }

    /**
     * 执行DAG核心逻辑
     */
    private void executeDag(List<DagNode> nodes, List<DagEdge> edges,
                           List<String> topoOrder, Map<String, List<DagEdge>> adjacency,
                           Map<String, Integer> inDegree, DagExecutionContext context, Long dbExecutionId) {
        Map<String, DagNode> nodeMap = new HashMap<>();
        for (DagNode node : nodes) {
            nodeMap.put(node.getId(), node);
        }

        // 使用CountDownLatch跟踪节点执行
        Map<String, CountDownLatch> nodeLatches = new HashMap<>();
        for (String nodeId : topoOrder) {
            nodeLatches.put(nodeId, new CountDownLatch(inDegree.getOrDefault(nodeId, 0)));
        }

        // 提交所有节点执行
        Map<String, Future<?>> futures = new HashMap<>();
        for (String nodeId : topoOrder) {
            DagNode node = nodeMap.get(nodeId);
            if (node == null) continue;

            Future<?> future = executorService.submit(() -> {
                try {
                    // 等待所有前置节点完成
                    CountDownLatch latch = nodeLatches.get(nodeId);
                    if (latch != null) {
                        boolean await = latch.await(30, TimeUnit.MINUTES);
                        if (!await) {
                            context.setNodeStatus(nodeId, "TIMEOUT");
                            return;
                        }
                    }

                    // 检查前置节点状态，决定是否跳过
                    if (shouldSkipNode(nodeId, edges, nodeMap, context)) {
                        context.setNodeStatus(nodeId, "SKIPPED");
                        log.info("[DagEngine] 节点跳过: nodeId={}, name={}", nodeId, node.getName());
                        publishEvent(DagExecutionEvent.EventType.NODE_SKIP, context.getTaskId(), dbExecutionId, nodeId, node.getName(), "SKIPPED", "节点被跳过");
                    } else {
                        // 执行节点
                        executeNodeWithContext(node, context, dbExecutionId);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    context.setNodeStatus(nodeId, "INTERRUPTED");
                } finally {
                    // 通知后置节点
                    List<DagEdge> outEdges = adjacency.getOrDefault(nodeId, Collections.emptyList());
                    for (DagEdge edge : outEdges) {
                        CountDownLatch nextLatch = nodeLatches.get(edge.getTarget());
                        if (nextLatch != null) {
                            nextLatch.countDown();
                        }
                    }
                }
            });
            futures.put(nodeId, future);
        }

        // 等待所有节点完成
        for (Future<?> future : futures.values()) {
            try {
                future.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("[DagEngine] 执行被中断");
            } catch (ExecutionException e) {
                log.error("[DagEngine] 节点执行异常", e);
            }
        }
    }

    /**
     * 判断节点是否应该跳过（基于条件分支）
     */
    private boolean shouldSkipNode(String nodeId, List<DagEdge> edges,
                                    Map<String, DagNode> nodeMap, DagExecutionContext context) {
        // 找到指向当前节点的所有边
        for (DagEdge edge : edges) {
            if (edge.getTarget().equals(nodeId)) {
                String sourceStatus = context.getNodeStatus(edge.getSource());
                // 如果源节点执行了但边的条件不匹配，则跳过
                if (!"PENDING".equals(sourceStatus) && !"RUNNING".equals(sourceStatus)) {
                    if (!edge.matchesCondition(sourceStatus)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 执行单个节点
     */
    private void executeNodeWithContext(DagNode node, DagExecutionContext context, Long dbExecutionId) {
        String nodeId = node.getId();
        context.setNodeStatus(nodeId, "RUNNING");
        log.info("[DagEngine] 节点开始执行: nodeId={}, type={}, name={}",
            nodeId, node.getType(), node.getName());

        // 发布节点开始事件
        publishEvent(DagExecutionEvent.EventType.NODE_START, context.getTaskId(), dbExecutionId, nodeId, node.getName(), "RUNNING", "节点开始执行");

        // 创建节点执行日志
        EtlNodeExecutionLog nodeLog = new EtlNodeExecutionLog();
        nodeLog.setExecutionId(dbExecutionId);
        nodeLog.setNodeId(nodeId);
        nodeLog.setNodeName(node.getName());
        nodeLog.setStatus("RUNNING");
        nodeLog.setStartTime(LocalDateTime.now());
        nodeExecutionLogMapper.insert(nodeLog);

        try {
            // 转换节点为EtlDagNode格式
            com.etl.engine.entity.EtlDagNode etlNode = convertToEtlDagNode(node);

            // 调用节点执行器
            String status = nodeExecutor.executeNode(etlNode, context.getVariables());

            context.setNodeStatus(nodeId, status);
            context.setNodeOutput(nodeId, status);

            // 更新节点日志
            nodeLog.setStatus(status);
            nodeLog.setEndTime(LocalDateTime.now());
            nodeLog.setLogContent("节点执行" + ("SUCCESS".equals(status) ? "成功" : status));
            nodeExecutionLogMapper.updateById(nodeLog);

            // 发布节点完成事件
            publishEvent(DagExecutionEvent.EventType.NODE_COMPLETE, context.getTaskId(), dbExecutionId, nodeId, node.getName(), status, "节点执行完成: " + status);

            log.info("[DagEngine] 节点执行完成: nodeId={}, status={}", nodeId, status);
        } catch (Exception e) {
            log.error("[DagEngine] 节点执行失败: nodeId={}", nodeId, e);
            context.setNodeStatus(nodeId, "FAILED");

            // 更新节点日志为失败
            nodeLog.setStatus("FAILED");
            nodeLog.setEndTime(LocalDateTime.now());
            nodeLog.setLogContent("节点执行失败: " + e.getMessage());
            nodeExecutionLogMapper.updateById(nodeLog);

            // 发布节点失败事件
            publishEvent(DagExecutionEvent.EventType.NODE_FAILED, context.getTaskId(), dbExecutionId, nodeId, node.getName(), "FAILED", e.getMessage());
        }
    }

    /**
     * 转换DagNode为EtlDagNode
     */
    private com.etl.engine.entity.EtlDagNode convertToEtlDagNode(DagNode node) {
        com.etl.engine.entity.EtlDagNode etlNode = new com.etl.engine.entity.EtlDagNode();
        etlNode.setNodeId(node.getId());
        etlNode.setNodeType(node.getType());
        etlNode.setNodeName(node.getName());
        etlNode.setConfig(node.getConfig());
        etlNode.setPositionX(node.getX());
        etlNode.setPositionY(node.getY());
        return etlNode;
    }

    /**
     * 构建执行结果
     */
    private DagExecutionResult buildResult(DagExecutionContext context, List<DagNode> nodes) {
        DagExecutionResult result = new DagExecutionResult();
        result.setExecutionId(context.getExecutionId());
        result.setTaskId(context.getTaskId());
        result.setDuration(context.getDurationSeconds());

        // 统计节点状态
        int success = 0, failed = 0, skipped = 0, interrupted = 0;
        for (DagNode node : nodes) {
            String status = context.getNodeStatus(node.getId());
            switch (status) {
                case "SUCCESS" -> success++;
                case "FAILED" -> failed++;
                case "SKIPPED" -> skipped++;
                case "INTERRUPTED" -> interrupted++;
            }
        }

        result.setTotalNodes(nodes.size());
        result.setSuccessNodes(success);
        result.setFailedNodes(failed);
        result.setSkippedNodes(skipped);
        result.setInterruptedNodes(interrupted);

        if (failed > 0) {
            result.setStatus("FAILED");
        } else if (interrupted > 0) {
            result.setStatus("INTERRUPTED");
        } else {
            result.setStatus("SUCCESS");
        }

        return result;
    }

    /**
     * 发布DAG执行事件
     */
    private void publishEvent(DagExecutionEvent.EventType eventType, Long taskId, Long executionId,
                              String nodeId, String nodeName, String status, String message) {
        if (eventPublisher != null) {
            eventPublisher.publishEvent(DagExecutionEvent.builder()
                .eventType(eventType)
                .taskId(taskId)
                .executionId(executionId)
                .nodeId(nodeId)
                .nodeName(nodeName)
                .status(status)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build());
        }
    }
}
