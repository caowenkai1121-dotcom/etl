package com.etl.monitor.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 同步日志详情实体
 */
@Data
@TableName("etl_sync_log_detail")
public class EtlSyncLogDetail implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联的主日志ID
     */
    private Long logId;

    /**
     * 链路追踪ID
     */
    private String traceId;

    /**
     * 步骤代码
     */
    private String stepCode;

    /**
     * 行号
     */
    private Integer rowIndex;

    /**
     * 源数据值
     */
    private String sourceValue;

    /**
     * 目标值
     */
    private String targetValue;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 是否出错：0-否，1-是
     */
    private Integer isError;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
