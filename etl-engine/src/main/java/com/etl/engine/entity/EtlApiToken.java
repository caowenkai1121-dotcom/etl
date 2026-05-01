package com.etl.engine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * API访问Token实体
 */
@Data
@TableName("etl_api_token")
public class EtlApiToken implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * Token值
     */
    private String token;

    /**
     * Token名称
     */
    private String name;

    /**
     * 应用ID
     */
    private String appId;

    /**
     * 允许IP列表(逗号分隔)
     */
    private String allowedIps;

    /**
     * 过期时间
     */
    private LocalDateTime expireTime;

    /**
     * 状态: 0-禁用 1-启用
     */
    private Integer status;

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
