package com.etl.engine.converter;

import com.etl.common.domain.FieldMapping;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 默认数据转换器实现
 */
@Slf4j
@Component
public class DefaultDataConverter implements DataConverter {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public Object convertValue(Object sourceValue, FieldMapping mapping) {
        if (sourceValue == null) {
            return null;
        }

        String targetType = mapping.getTargetType();
        if (targetType == null) {
            return sourceValue;
        }

        try {
            return switch (targetType.toUpperCase()) {
                case "VARCHAR", "CHAR", "TEXT", "STRING" -> convertToString(sourceValue);
                case "INT", "INTEGER", "SMALLINT", "TINYINT" -> convertToInteger(sourceValue);
                case "BIGINT" -> convertToLong(sourceValue);
                case "DECIMAL", "NUMERIC" -> convertToDecimal(sourceValue);
                case "FLOAT", "REAL" -> convertToFloat(sourceValue);
                case "DOUBLE" -> convertToDouble(sourceValue);
                case "BOOLEAN", "BOOL" -> convertToBoolean(sourceValue);
                case "DATE" -> convertToDate(sourceValue);
                case "DATETIME", "TIMESTAMP" -> convertToTimestamp(sourceValue);
                case "JSON" -> convertToJson(sourceValue);
                default -> sourceValue;
            };
        } catch (Exception e) {
            log.warn("字段转换失败: {} -> {}, 值: {}, 错误: {}",
                mapping.getSourceField(), mapping.getTargetField(), sourceValue, e.getMessage());
            return sourceValue;
        }
    }

    @Override
    public String convertType(String sourceType, String targetType) {
        // 简化实现，实际应从数据库配置读取映射
        return targetType;
    }

    @Override
    public Map<String, Object> convertRow(Map<String, Object> sourceRow, List<FieldMapping> mappings) {
        Map<String, Object> targetRow = new HashMap<>();

        for (FieldMapping mapping : mappings) {
            String sourceField = mapping.getSourceField();
            String targetField = mapping.getTargetField() != null ?
                mapping.getTargetField() : sourceField;

            Object sourceValue = sourceRow.get(sourceField);
            Object targetValue = convertValue(sourceValue, mapping);
            targetRow.put(targetField, targetValue);
        }

        return targetRow;
    }

    @Override
    public List<Map<String, Object>> convertBatch(List<Map<String, Object>> sourceData, List<FieldMapping> mappings) {
        return sourceData.stream()
            .map(row -> convertRow(row, mappings))
            .toList();
    }

    // ==================== 类型转换方法 ====================

    private String convertToString(Object value) {
        if (value instanceof byte[] bytes) {
            return new String(bytes);
        }
        return value.toString();
    }

    private Integer convertToInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(value.toString());
    }

    private Long convertToLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(value.toString());
    }

    private BigDecimal convertToDecimal(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return new BigDecimal(value.toString());
    }

    private Float convertToFloat(Object value) {
        if (value instanceof Number number) {
            return number.floatValue();
        }
        return Float.parseFloat(value.toString());
    }

    private Double convertToDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return Double.parseDouble(value.toString());
    }

    private Boolean convertToBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        return Boolean.parseBoolean(value.toString());
    }

    private String convertToDate(Object value) {
        if (value instanceof java.sql.Date date) {
            return date.toLocalDate().format(DATE_FORMATTER);
        }
        if (value instanceof LocalDateTime dateTime) {
            return dateTime.format(DATE_FORMATTER);
        }
        return value.toString();
    }

    private Timestamp convertToTimestamp(Object value) {
        if (value instanceof Timestamp timestamp) {
            return timestamp;
        }
        if (value instanceof java.sql.Date date) {
            return new Timestamp(date.getTime());
        }
        if (value instanceof LocalDateTime dateTime) {
            return Timestamp.valueOf(dateTime);
        }
        if (value instanceof Long epoch) {
            return new Timestamp(epoch);
        }
        return Timestamp.valueOf(value.toString());
    }

    private String convertToJson(Object value) {
        if (value instanceof String str) {
            return str;
        }
        return value.toString();
    }
}
