package com.etl.api.dto;
import lombok.Data;
import java.util.Map;
/**
 * 连接池状态响应DTO
 */
@Data
public class PoolStatusResponse {
    private Map<String, Map<String, Object>> pools;
    private int totalPools;
    private int activePools;
}
