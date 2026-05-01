package com.etl.engine.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * DAG配置响应
 */
@Data
public class DagConfigResponse {

    private Long id;

    private Long taskId;

    private Integer version;

    private List<NodeConfig> nodes;

    private List<EdgeConfig> edges;

    private ViewportConfig viewport;

    @Data
    public static class NodeConfig {
        private String id;
        private String type;
        private String name;
        private Integer x;
        private Integer y;
        private Map<String, Object> config;
        private Map<String, Object> inputSchema;
        private Map<String, Object> outputSchema;
    }

    @Data
    public static class EdgeConfig {
        private String id;
        private String source;
        private String target;
        private String sourcePort;
        private String targetPort;
        private String condition;
        private Map<String, Object> config;
    }

    @Data
    public static class ViewportConfig {
        private Double x;
        private Double y;
        private Double zoom;
    }
}
