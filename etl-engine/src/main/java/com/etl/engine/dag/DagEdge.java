package com.etl.engine.dag;

import lombok.Data;

/**
 * DAG边（连线）模型
 */
@Data
public class DagEdge {
    private String id;
    private String source;
    private String target;
    private String condition;

    public DagEdge() {}

    public DagEdge(String id, String source, String target, String condition) {
        this.id = id;
        this.source = source;
        this.target = target;
        this.condition = condition != null ? condition : "SUCCESS";
    }

    public boolean matchesCondition(String nodeStatus) {
        if ("ANY".equals(condition)) return true;
        if ("SUCCESS".equals(condition) && "SUCCESS".equals(nodeStatus)) return true;
        if ("FAILED".equals(condition) && "FAILED".equals(nodeStatus)) return true;
        return false;
    }
}
