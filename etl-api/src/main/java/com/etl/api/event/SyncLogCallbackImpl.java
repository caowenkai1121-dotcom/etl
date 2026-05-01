package com.etl.api.event;

import com.etl.common.callback.SyncLogCallback;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 同步日志回调实现
 * 通过Spring事件机制发布日志事件
 */
@Component
@RequiredArgsConstructor
public class SyncLogCallbackImpl implements SyncLogCallback {

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void log(Long taskId, Long executionId, String traceId, String level, String logType, String tableName, String message) {
        SyncLogEvent event = new SyncLogEvent(taskId, executionId, traceId, level, logType, tableName, message);
        eventPublisher.publishEvent(event);
    }

    @Override
    public void progress(Long taskId, Long executionId, String traceId, int progress, long totalRows, long successRows, long failedRows, String status) {
        SyncProgressEvent event = new SyncProgressEvent(taskId, executionId, traceId, progress, totalRows, successRows, failedRows, status);
        eventPublisher.publishEvent(event);
    }
}
