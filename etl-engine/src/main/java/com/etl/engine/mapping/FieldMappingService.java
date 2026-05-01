package com.etl.engine.mapping;

import cn.hutool.core.util.StrUtil;
import com.etl.common.domain.FieldMapping;
import com.etl.common.utils.JsonUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 字段映射服务
 */
public class FieldMappingService {

    /**
     * 解析字段映射配置
     */
    public List<FieldMapping> parseFieldMapping(String fieldMappingJson) {
        List<FieldMapping> mappings = new ArrayList<>();
        if (StrUtil.isBlank(fieldMappingJson)) {
            return mappings;
        }

        JSONArray array = JsonUtil.parseArray(fieldMappingJson);
        if (array == null) {
            return mappings;
        }

        for (int i = 0; i < array.size(); i++) {
            JSONObject obj = array.getJSONObject(i);
            FieldMapping mapping = new FieldMapping();
            mapping.setSourceField(obj.getString("sourceField"));
            mapping.setSourceType(obj.getString("sourceType"));
            mapping.setTargetField(obj.getString("targetField"));
            mapping.setTargetType(obj.getString("targetType"));
            mapping.setTransformExpression(obj.getString("transformExpression"));
            mapping.setPrimaryKey(obj.getBoolean("primaryKey"));
            mappings.add(mapping);
        }
        return mappings;
    }

    /**
     * 构建字段映射Map
     */
    public Map<String, String> buildMappingMap(List<FieldMapping> mappings) {
        Map<String, String> map = new HashMap<>();
        for (FieldMapping mapping : mappings) {
            map.put(mapping.getSourceField(), mapping.getTargetField());
        }
        return map;
    }

    /**
     * 应用数据转换
     */
    public Object transformValue(Object value, FieldMapping mapping) {
        if (value == null || StrUtil.isBlank(mapping.getTransformExpression())) {
            return value;
        }

        String expression = mapping.getTransformExpression();
        // 简单的转换表达式支持
        // 例如: UPPER, LOWER, TRIM, DATE_FORMAT等

        if ("UPPER".equalsIgnoreCase(expression)) {
            return value.toString().toUpperCase();
        } else if ("LOWER".equalsIgnoreCase(expression)) {
            return value.toString().toLowerCase();
        } else if ("TRIM".equalsIgnoreCase(expression)) {
            return value.toString().trim();
        }

        return value;
    }

    /**
     * 生成默认的字段映射
     */
    public String generateDefaultMapping(List<String> sourceColumns, List<String> targetColumns) {
        JSONArray array = new JSONArray();
        for (int i = 0; i < sourceColumns.size() && i < targetColumns.size(); i++) {
            JSONObject mapping = new JSONObject();
            mapping.put("sourceField", sourceColumns.get(i));
            mapping.put("targetField", targetColumns.get(i));
            mapping.put("sourceType", "VARCHAR");
            mapping.put("targetType", "VARCHAR");
            mapping.put("transformExpression", null);
            mapping.put("primaryKey", false);
            array.add(mapping);
        }
        return array.toJSONString();
    }
}
