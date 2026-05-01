package com.etl.engine.transform;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 数据清洗器
 */
@Component
public class DataCleanser {

    /**
     * 清洗数据
     *
     * @param data 原始数据
     * @param config 清洗配置
     * @return 清洗后的数据
     */
    public List<Map<String, Object>> cleanse(List<Map<String, Object>> data, CleanseConfig config) {
        if (data == null || data.isEmpty()) {
            return data;
        }

        List<Map<String, Object>> result = new ArrayList<>();

        for (Map<String, Object> row : data) {
            boolean skip = false;
            Map<String, Object> cleanedRow = new java.util.HashMap<>(row);

            for (Map.Entry<String, Object> entry : cleanedRow.entrySet()) {
                Object value = entry.getValue();

                // 空值处理
                if (value == null) {
                    if (config.getNullHandling() == NullHandling.SKIP) {
                        skip = true;
                        break;
                    } else if (config.getNullHandling() == NullHandling.DEFAULT_VALUE) {
                        entry.setValue(config.getDefaultValue());
                    }
                }
                // 非法字符处理
                else if (config.isRemoveInvalidChars() && value instanceof String) {
                    String cleaned = replaceInvalidChars((String) value, "");
                    entry.setValue(cleaned);
                }
            }

            if (!skip) {
                result.add(cleanedRow);
            }
        }

        return result;
    }

    /**
     * 替换非法字符的内部方法
     */
    private String replaceInvalidChars(String str, String replacement) {
        if (str == null) {
            return null;
        }
        return str.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", replacement);
    }

    /**
     * 使用默认配置清洗
     */
    public List<Map<String, Object>> cleanse(List<Map<String, Object>> data) {
        return cleanse(data, CleanseConfig.defaultConfig());
    }

    /**
     * 空值处理策略
     */
    public enum NullHandling {
        /**
         * 跳过该行
         */
        SKIP,
        /**
         * 使用默认值
         */
        DEFAULT_VALUE,
        /**
         * 保留null
         */
        KEEP_NULL
    }

    /**
     * 清洗配置
     */
    @Data
    public static class CleanseConfig {
        /**
         * 空值处理策略
         */
        private NullHandling nullHandling = NullHandling.KEEP_NULL;

        /**
         * 默认值
         */
        private Object defaultValue = null;

        /**
         * 是否移除非法字符
         */
        private boolean removeInvalidChars = true;

        /**
         * 创建默认配置
         */
        public static CleanseConfig defaultConfig() {
            return new CleanseConfig();
        }

        /**
         * 创建跳过空行的配置
         */
        public static CleanseConfig skipNullRows() {
            CleanseConfig config = new CleanseConfig();
            config.setNullHandling(NullHandling.SKIP);
            return config;
        }

        /**
         * 创建使用默认值的配置
         */
        public static CleanseConfig useDefaultValue(Object defaultValue) {
            CleanseConfig config = new CleanseConfig();
            config.setNullHandling(NullHandling.DEFAULT_VALUE);
            config.setDefaultValue(defaultValue);
            return config;
        }
    }
}
