package com.etl.api.websocket;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 同步日志WebSocket处理器
 * 用于向客户端实时推送同步日志
 */
@Slf4j
@Component
public class SyncLogWebSocketHandler implements WebSocketHandler {

    // 存储所有连接的会话，按任务ID分组
    private final Map<Long, Set<WebSocketSession>> taskSessions = new ConcurrentHashMap<>();
    // 存储会话与任务ID的映射
    private final Map<String, Long> sessionTaskMap = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("WebSocket连接建立: sessionId={}", session.getId());
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        if (message instanceof TextMessage) {
            String payload = ((TextMessage) message).getPayload();
            try {
                JSONObject json = JSON.parseObject(payload);
                String action = json.getString("action");
                Long taskId = json.getLong("taskId");

                if ("subscribe".equals(action) && taskId != null) {
                    // 订阅任务日志
                    subscribeTask(session, taskId);
                } else if ("unsubscribe".equals(action) && taskId != null) {
                    // 取消订阅
                    unsubscribeTask(session, taskId);
                }
            } catch (Exception e) {
                log.warn("解析WebSocket消息失败: {}", payload, e);
            }
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket传输错误: sessionId={}", session.getId(), exception);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("WebSocket连接关闭: sessionId={}, status={}", session.getId(), status);
        // 清理会话
        Long taskId = sessionTaskMap.remove(session.getId());
        if (taskId != null) {
            Set<WebSocketSession> sessions = taskSessions.get(taskId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    taskSessions.remove(taskId);
                }
            }
        }
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    /**
     * 订阅任务日志
     */
    private void subscribeTask(WebSocketSession session, Long taskId) {
        sessionTaskMap.put(session.getId(), taskId);
        taskSessions.computeIfAbsent(taskId, k -> ConcurrentHashMap.newKeySet()).add(session);
        log.info("订阅任务日志: sessionId={}, taskId={}", session.getId(), taskId);

        // 发送订阅确认
        sendMessage(session, createMessage("subscribed", taskId, null));
    }

    /**
     * 取消订阅任务日志
     */
    private void unsubscribeTask(WebSocketSession session, Long taskId) {
        sessionTaskMap.remove(session.getId());
        Set<WebSocketSession> sessions = taskSessions.get(taskId);
        if (sessions != null) {
            sessions.remove(session);
        }
        log.info("取消订阅任务日志: sessionId={}, taskId={}", session.getId(), taskId);
    }

    /**
     * 推送日志消息给订阅了指定任务的所有客户端
     */
    public void pushLog(Long taskId, String logLevel, String logType, String tableName, String message) {
        pushLog(taskId, null, logLevel, logType, tableName, message);
    }

    /**
     * 推送日志消息（含traceId）
     */
    public void pushLog(Long taskId, String traceId, String logLevel, String logType, String tableName, String message) {
        Set<WebSocketSession> sessions = taskSessions.get(taskId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        JSONObject logMsg = new JSONObject();
        logMsg.put("action", "log");
        logMsg.put("taskId", taskId);
        logMsg.put("traceId", traceId);
        logMsg.put("logLevel", logLevel);
        logMsg.put("logType", logType);
        logMsg.put("tableName", tableName);
        logMsg.put("message", message);
        logMsg.put("timestamp", System.currentTimeMillis());

        String jsonStr = logMsg.toJSONString();

        // 向所有订阅该任务的客户端推送
        sessions.removeIf(session -> {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(jsonStr));
                    return false;
                }
            } catch (IOException e) {
                log.warn("推送日志失败: sessionId={}", session.getId(), e);
            }
            return true; // 移除已关闭的会话
        });
    }

    /**
     * 推送进度更新
     */
    public void pushProgress(Long taskId, Integer progress, Long totalRows, Long successRows, Long failedRows, String status) {
        pushProgress(taskId, null, progress, totalRows, successRows, failedRows, status);
    }

    /**
     * 推送进度更新（含traceId）
     */
    public void pushProgress(Long taskId, String traceId, Integer progress, Long totalRows, Long successRows, Long failedRows, String status) {
        Set<WebSocketSession> sessions = taskSessions.get(taskId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        JSONObject progressMsg = new JSONObject();
        progressMsg.put("action", "progress");
        progressMsg.put("taskId", taskId);
        progressMsg.put("traceId", traceId);
        progressMsg.put("progress", progress);
        progressMsg.put("totalRows", totalRows);
        progressMsg.put("successRows", successRows);
        progressMsg.put("failedRows", failedRows);
        progressMsg.put("status", status);
        progressMsg.put("timestamp", System.currentTimeMillis());

        String jsonStr = progressMsg.toJSONString();

        sessions.removeIf(session -> {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(jsonStr));
                    return false;
                }
            } catch (IOException e) {
                log.warn("推送进度失败: sessionId={}", session.getId(), e);
            }
            return true;
        });
    }

    /**
     * 推送执行完成消息
     */
    public void pushComplete(Long taskId, String status, String errorMessage) {
        pushComplete(taskId, null, status, errorMessage);
    }

    /**
     * 推送执行完成消息（含traceId）
     */
    public void pushComplete(Long taskId, String traceId, String status, String errorMessage) {
        Set<WebSocketSession> sessions = taskSessions.get(taskId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        JSONObject completeMsg = new JSONObject();
        completeMsg.put("action", "complete");
        completeMsg.put("taskId", taskId);
        completeMsg.put("traceId", traceId);
        completeMsg.put("status", status);
        completeMsg.put("errorMessage", errorMessage);
        completeMsg.put("timestamp", System.currentTimeMillis());

        String jsonStr = completeMsg.toJSONString();

        sessions.removeIf(session -> {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(jsonStr));
                    return false;
                }
            } catch (IOException e) {
                log.warn("推送完成消息失败: sessionId={}", session.getId(), e);
            }
            return true;
        });
    }

    private void sendMessage(WebSocketSession session, String message) {
        try {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(message));
            }
        } catch (IOException e) {
            log.warn("发送消息失败: sessionId={}", session.getId(), e);
        }
    }

    private String createMessage(String action, Long taskId, Object data) {
        JSONObject msg = new JSONObject();
        msg.put("action", action);
        msg.put("taskId", taskId);
        if (data != null) {
            msg.put("data", data);
        }
        msg.put("timestamp", System.currentTimeMillis());
        return msg.toJSONString();
    }

    /**
     * 推送结构化进度消息
     */
    public void pushStructuredProgress(SyncProgressMessage progressMessage) {
        Set<WebSocketSession> sessions = taskSessions.get(progressMessage.getTaskId());
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        JSONObject progressMsg = new JSONObject();
        progressMsg.put("action", "structuredProgress");
        progressMsg.put("data", progressMessage);
        progressMsg.put("timestamp", System.currentTimeMillis());

        String jsonStr = progressMsg.toJSONString();

        sessions.removeIf(session -> {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(jsonStr));
                    return false;
                }
            } catch (IOException e) {
                log.warn("推送结构化进度失败: sessionId={}", session.getId(), e);
            }
            return true;
        });
    }

    /**
     * 推送结构化进度消息（简化参数版本）
     */
    public void pushStructuredProgress(Long taskId, Long executionId, String traceId, String status,
                                        BigDecimal progress, Long totalRows, Long processedRows,
                                        Long successRows, Long failedRows, Long elapsedSeconds,
                                        Integer estimatedRemainingSeconds, Double rowsPerSecond,
                                        String currentTable) {
        SyncProgressMessage message = new SyncProgressMessage();
        message.setTaskId(taskId);
        message.setExecutionId(executionId);
        message.setTraceId(traceId);
        message.setStatus(status);
        message.setProgress(progress);
        message.setTotalRows(totalRows);
        message.setProcessedRows(processedRows);
        message.setSuccessRows(successRows);
        message.setFailedRows(failedRows);
        message.setElapsedSeconds(elapsedSeconds);
        message.setEstimatedRemainingSeconds(estimatedRemainingSeconds);
        message.setRowsPerSecond(rowsPerSecond);
        message.setCurrentTable(currentTable);
        message.setTimestamp(LocalDateTime.now());
        pushStructuredProgress(message);
    }
}
