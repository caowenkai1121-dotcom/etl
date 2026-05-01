package com.etl.engine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务版本实体
 */
@Data
@TableName("etl_task_version")
public class EtlTaskVersion {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;

    private Integer version;

    private String config;

    private String changeLog;

    private String publishStatus;

    private LocalDateTime publishedAt;

    private String publishedBy;

    private String createdBy;

    private LocalDateTime createdAt;
}