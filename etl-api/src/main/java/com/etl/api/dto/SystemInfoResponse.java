package com.etl.api.dto;
import lombok.Data;
/**
 * 系统信息响应DTO
 */
@Data
public class SystemInfoResponse {
    private long jvmMaxMemory;
    private long jvmUsedMemory;
    private long jvmFreeMemory;
    private int availableProcessors;
    private long diskTotal;
    private long diskFree;
    private long uptimeMs;
}
