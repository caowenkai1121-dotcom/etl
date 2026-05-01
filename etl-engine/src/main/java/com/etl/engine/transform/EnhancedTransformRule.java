package com.etl.engine.transform;

import java.util.List;
import java.util.Map;

/**
 * 增强型转换规则
 * 扩展了原始 TransformRule，新增高级转换类型
 */
public class EnhancedTransformRule extends TransformRule {

    /** 转换类型扩展 */
    public enum EnhancedTransformType {
        FIELD_SPLIT("字段拆分"),
        FIELD_MERGE("字段合并"),
        ENCODE_CONVERT("编码转换"),
        TIMEZONE_CONVERT("时区转换"),
        UNIT_CONVERT("单位换算"),
        DEDUPLICATE("数据去重"),
        NULL_FILL("空值填充"),
        OUTLIER_DETECT("异常值检测"),
        SCRIPT("脚本转换"),
        DICT_LOOKUP("字典翻译"),
        API_LOOKUP("外部API查询"),
        TYPE_VALIDATE("类型校验"),
        RANGE_VALIDATE("值域校验"),
        UNIQUE_VALIDATE("唯一性校验"),
        REFERENCE_VALIDATE("引用完整性校验");

        private final String description;
        EnhancedTransformType(String description) { this.description = description; }
        public String getDescription() { return description; }
    }

    /** 脚本配置 */
    private String scriptContent;
    private String scriptLanguage; // GROOVY / JAVASCRIPT

    /** 字典映射配置 */
    private String dictCode;

    /** API查询配置 */
    private String apiUrl;
    private String apiMethod;
    private Map<String, Object> apiHeaders;
    private String apiResponsePath;

    /** 字段拆分配置 */
    private String splitSeparator;
    private List<String> splitTargetFields;

    /** 空值填充策略 */
    private NullFillStrategy nullFillStrategy;

    /** 值域校验 */
    private Object minValue;
    private Object maxValue;
    private List<Object> allowedValues;

    public enum NullFillStrategy {
        FIXED_VALUE("固定值填充"),
        PREVIOUS_VALUE("前值填充"),
        AVERAGE_VALUE("平均值填充"),
        CUSTOM_SCRIPT("脚本填充");

        private final String description;
        NullFillStrategy(String description) { this.description = description; }
        public String getDescription() { return description; }
    }
}
