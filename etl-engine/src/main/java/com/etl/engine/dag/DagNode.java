package com.etl.engine.dag;

import lombok.Data;

/**
 * DAG节点模型
 */
@Data
public class DagNode {
    private String id;
    private String type;
    private String name;
    private int x;
    private int y;
    private String config;
}
