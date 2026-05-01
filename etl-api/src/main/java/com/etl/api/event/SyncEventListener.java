package com.etl.api.event;

import com.etl.api.websocket.SyncLogWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 同步事件监听器
 * 监听日志和进度事件，通过WebSocket推送给客户端
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SyncEventListener {

    private final SyncLogWebSocketHandler webSocketHandler;

    @Async
    @EventListener
    public void onSyncLogEvent(SyncLogEvent event) {
        webSocketHandler.pushLog(
            event.getTaskId(),
            event.getLogLevel(),
            event.getLogType(),
            event.getTableName(),
            event.getMessage()
        );
    }

    @Async
    @EventListener
    public void onSyncProgressEvent(SyncProgressEvent event) {
        webSocketHandler.pushProgress(
            event.getTaskId(),
            event.getProgress(),
            event.getTotalRows(),
            event.getSuccessRows(),
            event.getFailedRows(),
            event.getStatus()
        );
    }
}
