package com.etl.engine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 转换步骤实体
 */
@Data
@TableName("etl_transform_step")
public class EtlTransformStep implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属流水线ID
     */
    private Long pipelineId;

    /**
     * 步骤编码
     */
    private String stepCode;

    /**
     * 步骤名称
     */
    private String stepName;

    /**
     * 规则类型
     */
    private String ruleType;

    /**
     * 规则配置(JSON格式)
     */
    private String ruleConfig;

    /**
     * 步骤顺序
     */
    private Integer stepOrder;

    /**
     * 下一步骤编码
     */
    private String nextStepCode;

    /**
     * 错误策略(ABORT/IGNORE/CONTINUE)
     */
    private String errorStrategy;

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
}
