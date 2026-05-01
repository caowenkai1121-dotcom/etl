package com.etl.engine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("etl_audit_log")
public class EtlAuditLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String operator;
    private String operationType;
    private String targetType;
    private Long targetId;
    private String targetName;
    private String detail;
    private String ipAddress;
    private String userAgent;
    private String status;
    private String errorMessage;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
