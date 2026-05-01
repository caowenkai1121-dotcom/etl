package com.etl.api.dto;
import lombok.Data;
/**
 * 线程池状态响应DTO
 */
@Data
public class ThreadPoolStatusResponse {
    private int corePoolSize;
    private int maximumPoolSize;
    private int activeCount;
    private int poolSize;
    private int queueSize;
    private long completedTaskCount;
    private int availablePermits;
}
