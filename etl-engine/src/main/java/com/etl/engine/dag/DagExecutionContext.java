package com.etl.engine.dag;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DAG执行上下文
 * 保存执行过程中的变量、状态、中间结果
 */
@Slf4j
@Data
public class DagExecutionContext {

    // 执行实例ID
    private Long executionId;

    // 任务ID
    private Long taskId;

    // 全局变量表
    private final Map<String, Object> variables = new ConcurrentHashMap<>();

    // 节点输出结果表
    private final Map<String, Object> nodeOutputs = new ConcurrentHashMap<>();

    // 节点执行状态表
    private final Map<String, String> nodeStatus = new ConcurrentHashMap<>();

    // 开始时间
    private LocalDateTime startTime;

    // 结束时间
    private LocalDateTime endTime;

    // 运行参数
    private Map<String, Object> runParams;

    // 是否调试模式
    private boolean debugMode;

    public DagExecutionContext(Long executionId, Long taskId) {
        this.executionId = executionId;
        this.taskId = taskId;
        this.startTime = LocalDateTime.now();
    }

    /**
     * 设置变量
     */
    public void setVariable(String key, Object value) {
        variables.put(key, value);
        log.debug("[Context] 设置变量: {}={}", key, value);
    }

    /**
     * 获取变量
     */
    public Object getVariable(String key) {
        return variables.get(key);
    }

    /**
     * 获取变量（带默认值）
     */
    public Object getVariable(String key, Object defaultValue) {
        return variables.getOrDefault(key, defaultValue);
    }

    /**
     * 设置节点输出
     */
    public void setNodeOutput(String nodeId, Object output) {
        nodeOutputs.put(nodeId, output);
    }

    /**
     * 获取节点输出
     */
    public Object getNodeOutput(String nodeId) {
        return nodeOutputs.get(nodeId);
    }

    /**
     * 设置节点状态
     */
    public void setNodeStatus(String nodeId, String status) {
        nodeStatus.put(nodeId, status);
    }

    /**
     * 获取节点状态
     */
    public String getNodeStatus(String nodeId) {
        return nodeStatus.getOrDefault(nodeId, "PENDING");
    }

    /**
     * 获取运行时长（秒）
     */
    public long getDurationSeconds() {
        if (startTime == null) return 0;
        LocalDateTime end = endTime != null ? endTime : LocalDateTime.now();
        return java.time.Duration.between(startTime, end).getSeconds();
    }
}
