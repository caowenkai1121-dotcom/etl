package com.etl.engine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("etl_task_dependency")
public class EtlTaskDependency {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Long dependsOnTaskId;
    private String dependencyType; // FINISH / SUCCESS
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
