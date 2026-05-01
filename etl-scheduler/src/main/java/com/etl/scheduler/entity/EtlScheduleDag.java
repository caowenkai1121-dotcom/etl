package com.etl.scheduler.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("etl_schedule_dag")
public class EtlScheduleDag {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String dagName;
    private String description;
    private String dagConfig;
    private String cronExpression;
    private Integer status;
    private String createBy;
    private LocalDateTime createTime;
    private String updateBy;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
