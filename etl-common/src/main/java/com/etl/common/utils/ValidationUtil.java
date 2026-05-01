package com.etl.common.utils;

/**
 * 验证工具类
 */
public class ValidationUtil {

    private ValidationUtil() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    /**
     * 检查对象是否不为null
     */
    public static boolean isNotNull(Object obj) {
        return obj != null;
    }

    /**
     * 检查数值是否在指定范围内（包含边界）
     */
    public static boolean isInRange(Number number, Number min, Number max) {
        if (number == null || min == null || max == null) {
            return false;
        }
        double num = number.doubleValue();
        double minVal = min.doubleValue();
        double maxVal = max.doubleValue();
        return num >= minVal && num <= maxVal;
    }

    /**
     * 检查字符串是否匹配正则表达式
     */
    public static boolean matchesRegex(String str, String regex) {
        if (str == null || regex == null) {
            return false;
        }
        return str.matches(regex);
    }

    /**
     * 检查字符串长度是否在指定范围内
     */
    public static boolean isLengthInRange(String str, int minLength, int maxLength) {
        if (str == null) {
            return false;
        }
        int length = str.length();
        return length >= minLength && length <= maxLength;
    }

    /**
     * 检查值是否在枚举范围内
     */
    public static <T> boolean isInEnum(T value, T[] enumValues) {
        if (value == null || enumValues == null || enumValues.length == 0) {
            return false;
        }
        for (T enumValue : enumValues) {
            if (value.equals(enumValue)) {
                return true;
            }
        }
        return false;
    }
}
