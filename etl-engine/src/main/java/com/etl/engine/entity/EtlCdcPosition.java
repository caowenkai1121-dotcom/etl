package com.etl.engine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * CDC同步位点实体
 */
@Data
@TableName("etl_cdc_position")
public class EtlCdcPosition implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 任务ID
     */
    private Long taskId;

    /**
     * 源数据源ID
     */
    private Long sourceDsId;

    /**
     * 表名
     */
    private String tableName;

    /**
     * 位点类型
     */
    private String positionType;

    /**
     * 位点值
     */
    private String positionValue;

    /**
     * Binlog文件名(MySQL)
     */
    private String binlogFile;

    /**
     * Binlog位置(MySQL)
     */
    private Long binlogPosition;

    /**
     * GTID(MySQL)
     */
    private String gtid;

    /**
     * LSN(PostgreSQL)
     */
    private String lsn;

    /**
     * 扩展信息(JSON格式)
     */
    private String extra;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
