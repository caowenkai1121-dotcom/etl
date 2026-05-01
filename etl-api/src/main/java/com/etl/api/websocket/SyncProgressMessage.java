package com.etl.api.websocket;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 同步进度消息
 */
@Data
public class SyncProgressMessage {

    private Long taskId;
    private Long executionId;
    private String traceId;
    private String status;
    private BigDecimal progress;
    private Long totalRows;
    private Long processedRows;
    private Long successRows;
    private Long failedRows;
    private Long elapsedSeconds;
    private Integer estimatedRemainingSeconds;
    private Double rowsPerSecond;
    private String currentTable;
    private LocalDateTime timestamp;
}
