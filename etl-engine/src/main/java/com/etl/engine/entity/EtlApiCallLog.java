package com.etl.engine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * API调用日志实体
 */
@Data
@TableName("etl_api_call_log")
public class EtlApiCallLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * API服务ID
     */
    private Long apiId;

    /**
     * API名称
     */
    private String apiName;

    /**
     * 请求时间
     */
    private LocalDateTime requestTime;

    /**
     * 请求IP
     */
    private String requestIp;

    /**
     * 请求参数
     */
    private String requestParams;

    /**
     * 响应状态码
     */
    private Integer responseCode;

    /**
     * 响应时间(毫秒)
     */
    private Integer responseTime;

    /**
     * 返回行数
     */
    private Integer responseRows;

    /**
     * 状态: SUCCESS/FAILED
     */
    private String status;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
