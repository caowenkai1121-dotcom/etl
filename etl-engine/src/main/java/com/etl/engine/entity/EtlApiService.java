package com.etl.engine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * API服务定义实体
 */
@Data
@TableName("etl_api_service")
public class EtlApiService implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * API名称
     */
    private String name;

    /**
     * API路径
     */
    private String path;

    /**
     * 请求方法: GET/POST
     */
    private String method;

    /**
     * 关联数据源ID
     */
    private Long datasourceId;

    /**
     * SQL模板
     */
    private String sqlTemplate;

    /**
     * 参数配置JSON
     */
    private String paramsConfig;

    /**
     * 认证方式: TOKEN/IP/SIGN/NONE
     */
    private String authType;

    /**
     * 认证配置
     */
    private String authConfig;

    /**
     * 限流(次/分钟)
     */
    private Integer rateLimit;

    /**
     * 超时时间(秒)
     */
    private Integer timeout;

    /**
     * 状态: ONLINE/OFFLINE
     */
    private String status;

    /**
     * 描述
     */
    private String description;

    /**
     * 所属文件夹ID
     */
    private Long folderId;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新人
     */
    private String updateBy;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除
     */
    @TableLogic
    private Integer deleted;
}
