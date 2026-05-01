package com.etl.monitor.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 质量校验报告实体
 */
@Data
@TableName("etl_quality_report")
public class EtlQualityReport implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 任务ID
     */
    private Long taskId;

    /**
     * 执行记录ID
     */
    private Long executionId;

    /**
     * 规则ID
     */
    private Long ruleId;

    /**
     * 质量评分(0-100)
     */
    private BigDecimal qualityScore;

    /**
     * 总校验数
     */
    private Long totalCount;

    /**
     * 通过数
     */
    private Long passCount;

    /**
     * 失败数
     */
    private Long failCount;

    /**
     * 失败样本(JSON)
     */
    private String failSamples;

    /**
     * 报告时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime reportTime;
}
