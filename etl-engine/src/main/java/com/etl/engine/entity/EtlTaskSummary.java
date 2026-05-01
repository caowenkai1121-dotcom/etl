package com.etl.engine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("etl_task_summary")
public class EtlTaskSummary {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private LocalDate summaryDate;
    private Integer totalExecutions;
    private Integer successExecutions;
    private Integer failedExecutions;
    private Long avgDuration;
    private Long totalRows;
    private Long successRows;
    private Long failedRows;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
