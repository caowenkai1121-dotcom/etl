package com.etl.engine.load;

import com.etl.common.context.SyncContext;
import com.etl.common.pipeline.Loader;
import lombok.extern.slf4j.Slf4j;
import java.util.*;

@Slf4j
public class SmartBatchLoader implements Loader {
    private final String targetDsType;
    private final int batchSize;
    private final LoaderDelegate delegate;

    public SmartBatchLoader(String targetDsType, int batchSize, LoaderDelegate delegate) {
        this.targetDsType = targetDsType;
        this.batchSize = batchSize;
        this.delegate = delegate;
    }

    @Override
    public String getName() { return "SmartBatchLoader-" + targetDsType; }

    @Override
    public void load(SyncContext context) {
        List<Map<String, Object>> data = context.getTransformedData();
        if (data == null || data.isEmpty()) return;
        List<List<Map<String, Object>>> batches = partition(data, batchSize);
        for (List<Map<String, Object>> batch : batches) {
            if (context.isInterrupted()) break;
            switch (targetDsType.toUpperCase()) {
                case "MYSQL" -> delegate.batchInsertOnDuplicate(batch, context);
                case "POSTGRESQL" -> delegate.batchCopy(batch, context);
                case "DORIS" -> delegate.streamLoad(batch, context);
                default -> delegate.batchInsert(batch, context);
            }
            context.incrementLoaded(batch.size());
        }
    }

    private <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            result.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return result;
    }

    public interface LoaderDelegate {
        void batchInsertOnDuplicate(List<Map<String, Object>> batch, SyncContext context);
        void batchCopy(List<Map<String, Object>> batch, SyncContext context);
        void streamLoad(List<Map<String, Object>> batch, SyncContext context);
        void batchInsert(List<Map<String, Object>> batch, SyncContext context);
    }
}