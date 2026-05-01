package com.etl.engine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 同步任务实体
 */
@Data
@TableName("etl_sync_task")
public class EtlSyncTask implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 任务名称
     */
    private String name;

    /**
     * 任务名称(用于开发模块)
     */
    private String taskName;

    /**
     * 所属文件夹ID
     */
    private Long folderId;

    /**
     * 发布状态: DRAFT/PENDING/PUBLISHED/UPDATED
     */
    private String publishStatus;

    /**
     * 开发环境配置JSON
     */
    private String devConfig;

    /**
     * 生产环境配置JSON
     */
    private String prodConfig;

    /**
     * 调度配置JSON
     */
    private String scheduleConfig;

    /**
     * 最后发布时间
     */
    private LocalDateTime lastPublishTime;

    /**
     * 任务描述
     */
    private String description;

    /**
     * 源数据源ID
     */
    private Long sourceDsId;

    /**
     * 目标数据源ID
     */
    private Long targetDsId;

    /**
     * 同步模式
     */
    private String syncMode;

    /**
     * 同步范围
     */
    private String syncScope;

    /**
     * 表配置
     */
    private String tableConfig;

    /**
     * 字段映射配置
     */
    private String fieldMapping;

    /**
     * 增量同步字段
     */
    private String incrementalField;

    /**
     * 增量同步策略类型
     */
    private String incrementalType;

    /**
     * 增量同步的起始值
     */
    private String incrementalValue;

    /**
     * Cron表达式
     */
    private String cronExpression;

    /**
     * 同步策略
     */
    private String syncStrategy;

    /**
     * 批量处理大小
     */
    private Integer batchSize;

    /**
     * 并行线程数
     */
    private Integer parallelThreads;

    /**
     * 失败重试次数
     */
    private Integer retryTimes;

    /**
     * 重试间隔(秒)
     */
    private Integer retryInterval;

    /**
     * 状态
     */
    private String status;

    /**
     * 最后同步时间
     */
    private LocalDateTime lastSyncTime;

    /**
     * 下次同步时间
     */
    private LocalDateTime nextSyncTime;

    /**
     * 创建人
     */
    private String createdBy;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新人
     */
    private String updatedBy;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 逻辑删除
     */
    @TableLogic
    private Integer deleted;

    /**
     * 是否收藏
     */
    private Integer isFavorite;
}
