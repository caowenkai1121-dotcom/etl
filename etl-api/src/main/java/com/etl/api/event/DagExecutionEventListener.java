package com.etl.api.event;

import com.etl.api.websocket.SyncLogWebSocketHandler;
import com.etl.engine.event.DagExecutionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * DAG执行事件监听器
 * 将执行事件转发到WebSocket推送
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DagExecutionEventListener {

    private final SyncLogWebSocketHandler webSocketHandler;

    @EventListener
    public void onDagExecutionEvent(DagExecutionEvent event) {
        try {
            Long taskId = event.getTaskId();
            Long executionId = event.getExecutionId();
            String nodeName = event.getNodeName();
            String message = event.getMessage();

            switch (event.getEventType()) {
                case TASK_START:
                    webSocketHandler.pushLog(taskId, "INFO", "TASK", null,
                        "[任务开始] executionId=" + executionId);
                    break;
                case TASK_COMPLETE:
                    webSocketHandler.pushLog(taskId, "INFO", "TASK", null,
                        "[任务完成] status=" + event.getStatus());
                    webSocketHandler.pushComplete(taskId, event.getStatus(), null);
                    break;
                case TASK_FAILED:
                    webSocketHandler.pushLog(taskId, "ERROR", "TASK", null,
                        "[任务失败] " + message);
                    webSocketHandler.pushComplete(taskId, "FAILED", message);
                    break;
                case NODE_START:
                    webSocketHandler.pushLog(taskId, "INFO", "NODE", nodeName,
                        "[节点开始] " + nodeName);
                    break;
                case NODE_COMPLETE:
                    webSocketHandler.pushLog(taskId, "SUCCESS", "NODE", nodeName,
                        "[节点完成] " + nodeName + " status=" + event.getStatus());
                    break;
                case NODE_FAILED:
                    webSocketHandler.pushLog(taskId, "ERROR", "NODE", nodeName,
                        "[节点失败] " + nodeName + " error=" + message);
                    break;
                case NODE_SKIP:
                    webSocketHandler.pushLog(taskId, "WARN", "NODE", nodeName,
                        "[节点跳过] " + nodeName);
                    break;
                default:
                    log.debug("未知事件类型: {}", event.getEventType());
            }
        } catch (Exception e) {
            log.error("处理DAG执行事件失败", e);
        }
    }
}
