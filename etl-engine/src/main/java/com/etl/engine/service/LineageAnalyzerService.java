package com.etl.engine.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.etl.engine.dag.DagEdge;
import com.etl.engine.dag.DagNode;
import com.etl.engine.entity.EtlDataLineage;
import com.etl.engine.mapper.DataLineageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 数据血缘分析服务
 * 解析DAG配置，提取并记录数据血缘关系
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LineageAnalyzerService {

    private final DataLineageMapper dataLineageMapper;

    // 输入型节点（数据来源）
    private static final Set<String> SOURCE_NODE_TYPES = Set.of(
        "DB_SYNC", "API_SYNC", "SERVER_DS", "FILE_READ", "JIANADAOYUN",
        "FTP_UPLOAD", "SFTP", "LOCAL_FILE"
    );

    // 输出型节点（数据目标）
    private static final Set<String> TARGET_NODE_TYPES = Set.of(
        "DB_OUTPUT", "API_OUTPUT", "FILE_WRITE", "FTP_DOWNLOAD"
    );

    // 转换型节点
    private static final Set<String> TRANSFORM_NODE_TYPES = Set.of(
        "FIELD_SELECT", "FIELD_RENAME", "FIELD_CALC", "DATA_FILTER",
        "DATA_AGG", "DATA_JOIN", "DATA_SORT", "DATA_DEDUP",
        "FIELD_SPLIT", "NULL_HANDLE", "JSON_PARSE", "XML_PARSE",
        "DATA_COMPARE", "VISUAL_TRANSFORM"
    );

    /**
     * 分析并保存DAG的血缘关系
     */
    @Transactional(rollbackFor = Exception.class)
    public void analyzeAndSave(Long taskId, List<DagNode> nodes, List<DagEdge> edges) {
        if (taskId == null || nodes == null || nodes.isEmpty()) {
            return;
        }

        // 清除该任务旧的血缘记录
        dataLineageMapper.delete(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<EtlDataLineage>()
                .eq(EtlDataLineage::getTaskId, taskId)
        );

        // 构建节点映射和邻接表
        Map<String, DagNode> nodeMap = new HashMap<>();
        for (DagNode node : nodes) {
            nodeMap.put(node.getId(), node);
        }

        Map<String, List<String>> adjacency = new HashMap<>();
        for (DagEdge edge : edges) {
            adjacency.computeIfAbsent(edge.getSource(), k -> new ArrayList<>()).add(edge.getTarget());
        }

        // 遍历所有节点，提取血缘关系
        List<EtlDataLineage> lineageList = new ArrayList<>();

        for (DagNode node : nodes) {
            String nodeType = node.getType();
            if (nodeType == null) continue;

            // 解析节点配置
            NodeConfig config = parseConfig(node.getConfig());

            if (SOURCE_NODE_TYPES.contains(nodeType)) {
                // 输入节点：记录为source，并查找下游目标
                List<String> downstream = findDownstreamTargets(node.getId(), adjacency, nodeMap);
                for (String targetId : downstream) {
                    DagNode targetNode = nodeMap.get(targetId);
                    NodeConfig targetConfig = parseConfig(targetNode.getConfig());
                    EtlDataLineage lineage = new EtlDataLineage();
                    lineage.setTaskId(taskId);
                    lineage.setNodeId(node.getId());
                    lineage.setNodeName(node.getName());
                    lineage.setNodeType(nodeType);
                    lineage.setSourceDatasourceId(config.datasourceId);
                    lineage.setSourceTable(config.tableName);
                    lineage.setSourceField(config.fieldName);
                    lineage.setTargetDatasourceId(targetConfig.datasourceId);
                    lineage.setTargetTable(targetConfig.tableName);
                    lineage.setTransformLogic(buildTransformLogic(nodeMap, node.getId(), targetId, adjacency));
                    lineageList.add(lineage);
                }
            } else if (TARGET_NODE_TYPES.contains(nodeType)) {
                // 输出节点：记录为target，并查找上游来源
                List<String> upstream = findUpstreamSources(node.getId(), edges, nodeMap);
                for (String sourceId : upstream) {
                    DagNode sourceNode = nodeMap.get(sourceId);
                    NodeConfig sourceConfig = parseConfig(sourceNode.getConfig());
                    EtlDataLineage lineage = new EtlDataLineage();
                    lineage.setTaskId(taskId);
                    lineage.setNodeId(node.getId());
                    lineage.setNodeName(node.getName());
                    lineage.setNodeType(nodeType);
                    lineage.setSourceDatasourceId(sourceConfig.datasourceId);
                    lineage.setSourceTable(sourceConfig.tableName);
                    lineage.setSourceField(sourceConfig.fieldName);
                    lineage.setTargetDatasourceId(config.datasourceId);
                    lineage.setTargetTable(config.tableName);
                    lineage.setTransformLogic(buildTransformLogic(nodeMap, sourceId, node.getId(), adjacency));
                    lineageList.add(lineage);
                }
            } else if (TRANSFORM_NODE_TYPES.contains(nodeType)) {
                // 转换节点：记录上下游关系
                List<String> upstream = findUpstreamSources(node.getId(), edges, nodeMap);
                List<String> downstream = findDownstreamTargets(node.getId(), adjacency, nodeMap);
                for (String sourceId : upstream) {
                    for (String targetId : downstream) {
                        DagNode sourceNode = nodeMap.get(sourceId);
                        DagNode targetNode = nodeMap.get(targetId);
                        NodeConfig sourceConfig = parseConfig(sourceNode.getConfig());
                        NodeConfig targetConfig = parseConfig(targetNode.getConfig());
                        EtlDataLineage lineage = new EtlDataLineage();
                        lineage.setTaskId(taskId);
                        lineage.setNodeId(node.getId());
                        lineage.setNodeName(node.getName());
                        lineage.setNodeType(nodeType);
                        lineage.setSourceDatasourceId(sourceConfig.datasourceId);
                        lineage.setSourceTable(sourceConfig.tableName);
                        lineage.setTargetDatasourceId(targetConfig.datasourceId);
                        lineage.setTargetTable(targetConfig.tableName);
                        lineage.setTransformLogic(node.getName());
                        lineageList.add(lineage);
                    }
                }
            }
        }

        // 批量保存
        if (!lineageList.isEmpty()) {
            for (EtlDataLineage lineage : lineageList) {
                dataLineageMapper.insert(lineage);
            }
            log.info("[Lineage] 任务血缘分析完成: taskId={}, 记录数={}", taskId, lineageList.size());
        }
    }

    /**
     * 查找下游目标节点（输出节点）
     */
    private List<String> findDownstreamTargets(String nodeId, Map<String, List<String>> adjacency, Map<String, DagNode> nodeMap) {
        List<String> targets = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new LinkedList<>();
        queue.offer(nodeId);
        visited.add(nodeId);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            List<String> neighbors = adjacency.getOrDefault(current, Collections.emptyList());
            for (String neighbor : neighbors) {
                if (visited.contains(neighbor)) continue;
                visited.add(neighbor);

                DagNode neighborNode = nodeMap.get(neighbor);
                if (neighborNode != null && TARGET_NODE_TYPES.contains(neighborNode.getType())) {
                    targets.add(neighbor);
                } else {
                    queue.offer(neighbor);
                }
            }
        }
        return targets;
    }

    /**
     * 查找上游来源节点（输入节点）
     */
    private List<String> findUpstreamSources(String nodeId, List<DagEdge> edges, Map<String, DagNode> nodeMap) {
        List<String> sources = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new LinkedList<>();
        queue.offer(nodeId);
        visited.add(nodeId);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            for (DagEdge edge : edges) {
                if (edge.getTarget().equals(current)) {
                    String source = edge.getSource();
                    if (visited.contains(source)) continue;
                    visited.add(source);

                    DagNode sourceNode = nodeMap.get(source);
                    if (sourceNode != null && SOURCE_NODE_TYPES.contains(sourceNode.getType())) {
                        sources.add(source);
                    } else {
                        queue.offer(source);
                    }
                }
            }
        }
        return sources;
    }

    /**
     * 构建转换逻辑描述
     */
    private String buildTransformLogic(Map<String, DagNode> nodeMap, String sourceId, String targetId,
                                       Map<String, List<String>> adjacency) {
        StringBuilder sb = new StringBuilder();
        // 简单记录中间经过的转换节点
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new LinkedList<>();
        queue.offer(sourceId);
        visited.add(sourceId);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            if (current.equals(targetId)) break;

            List<String> neighbors = adjacency.getOrDefault(current, Collections.emptyList());
            for (String neighbor : neighbors) {
                if (visited.contains(neighbor)) continue;
                visited.add(neighbor);

                DagNode node = nodeMap.get(neighbor);
                if (node != null && TRANSFORM_NODE_TYPES.contains(node.getType())) {
                    if (sb.length() > 0) sb.append(" → ");
                    sb.append(node.getName());
                }
                queue.offer(neighbor);
            }
        }
        return sb.length() > 0 ? sb.toString() : "直接传输";
    }

    /**
     * 解析节点配置JSON
     */
    private NodeConfig parseConfig(String configJson) {
        NodeConfig config = new NodeConfig();
        if (configJson == null || configJson.isEmpty()) {
            return config;
        }
        try {
            JSONObject json = JSON.parseObject(configJson);
            config.datasourceId = json.getLong("datasourceId");
            config.tableName = json.getString("tableName");
            config.fieldName = json.getString("fieldName");
            if (config.tableName == null) {
                config.tableName = json.getString("table");
            }
            if (config.fieldName == null) {
                config.fieldName = json.getString("field");
            }
        } catch (Exception e) {
            log.warn("解析节点配置失败: {}", configJson);
        }
        return config;
    }

    /**
     * 节点配置内部类
     */
    private static class NodeConfig {
        Long datasourceId;
        String tableName;
        String fieldName;
    }
}
