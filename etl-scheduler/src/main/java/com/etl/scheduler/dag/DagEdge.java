package com.etl.scheduler.dag;

import lombok.Data;

@Data
public class DagEdge {
    private String from;
    private String to;
    private String condition; // 触发条件
}
