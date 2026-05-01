package com.etl.engine.rollback;

import com.etl.common.context.SyncContext;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class CompensationRollback {
    private final List<Map<String, Object>> loadedRecords = new ArrayList<>();
    private final RollbackDelegate delegate;

    public CompensationRollback(RollbackDelegate delegate) {
        this.delegate = delegate;
    }

    public void recordLoaded(Map<String, Object> record) {
        loadedRecords.add(record);
    }

    public void rollback(SyncContext context) {
        log.warn("[Rollback] 开始补偿回滚, 共{}条记录", loadedRecords.size());
        for (Map<String, Object> record : loadedRecords) {
            try {
                delegate.deleteRecord(record, context);
            } catch (Exception e) {
                log.error("[Rollback] 回滚记录失败: {}", record, e);
            }
        }
        loadedRecords.clear();
        log.info("[Rollback] 补偿回滚完成");
    }

    public interface RollbackDelegate {
        void deleteRecord(Map<String, Object> record, SyncContext context);
    }
}
