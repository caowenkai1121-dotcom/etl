package com.etl.engine.converter;

import com.etl.common.domain.FieldMapping;

import java.util.List;
import java.util.Map;

/**
 * 数据转换器接口
 */
public interface DataConverter {

    /**
     * 转换单个字段值
     *
     * @param sourceValue 源值
     * @param mapping     字段映射配置
     * @return 转换后的值
     */
    Object convertValue(Object sourceValue, FieldMapping mapping);

    /**
     * 转换字段类型
     *
     * @param sourceType 源类型
     * @param targetType 目标类型
     * @return 目标类型字符串
     */
    String convertType(String sourceType, String targetType);

    /**
     * 转换整行数据
     *
     * @param sourceRow 源数据行
     * @param mappings  字段映射列表
     * @return 转换后的数据行
     */
    Map<String, Object> convertRow(Map<String, Object> sourceRow, List<FieldMapping> mappings);

    /**
     * 批量转换数据
     *
     * @param sourceData 源数据列表
     * @param mappings   字段映射列表
     * @return 转换后的数据列表
     */
    List<Map<String, Object>> convertBatch(List<Map<String, Object>> sourceData, List<FieldMapping> mappings);
}
