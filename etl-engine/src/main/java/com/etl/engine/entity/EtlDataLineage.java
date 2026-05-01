package com.etl.engine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 数据血缘关系实体
 */
@Data
@TableName("etl_data_lineage")
public class EtlDataLineage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;

    private String nodeId;

    private String nodeName;

    private String nodeType;

    private Long sourceDatasourceId;

    private String sourceTable;

    private String sourceField;

    private Long targetDatasourceId;

    private String targetTable;

    private String targetField;

    private String transformLogic;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
