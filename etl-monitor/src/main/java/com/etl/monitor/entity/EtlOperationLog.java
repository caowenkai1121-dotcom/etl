package com.etl.monitor.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作日志实体
 */
@Data
@TableName("etl_operation_log")
public class EtlOperationLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户名
     */
    @TableField("user_name")
    private String userName;

    /**
     * 操作类型
     */
    @TableField("operation")
    private String operation;

    /**
     * 模块名称
     */
    @TableField("module")
    private String module;

    /**
     * 目标ID
     */
    @TableField("target_id")
    private Long targetId;

    /**
     * 目标名称
     */
    @TableField("target_name")
    private String targetName;

    /**
     * 操作详情
     */
    @TableField("detail")
    private String detail;

    /**
     * 操作IP地址
     */
    @TableField("ip")
    private String ip;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 是否删除
     */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
