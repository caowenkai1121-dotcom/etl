package com.etl.common.context;

import com.etl.common.domain.TableInfo;
import lombok.Data;
import java.util.*;

@Data
public class SyncContext {
    private Long taskId;
    private Long executionId;
    private String traceId;
    private Map<String, Object> taskConfig = new HashMap<>();
    private List<Map<String, Object>> extractedData = new ArrayList<>();
    private List<Map<String, Object>> transformedData = new ArrayList<>();
    private TableInfo sourceTable;
    private TableInfo targetTable;
    private long extractedCount = 0;
    private long transformedCount = 0;
    private long loadedCount = 0;
    private long errorCount = 0;
    private long startTime;
    private Map<String, Object> metrics = new HashMap<>();
    private boolean interrupted = false;

    public void incrementExtracted(int count) { this.extractedCount += count; }
    public void incrementTransformed(int count) { this.transformedCount += count; }
    public void incrementLoaded(int count) { this.loadedCount += count; }
    public void incrementError() { this.errorCount++; }
}
