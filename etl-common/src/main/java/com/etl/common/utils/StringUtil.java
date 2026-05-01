package com.etl.common.utils;

/**
 * 字符串工具类
 */
public class StringUtil {

    private StringUtil() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    /**
     * 手机号脱敏
     * 格式：138****1234
     */
    public static String desensitizePhone(String phone) {
        if (isBlank(phone) || phone.length() != 11) {
            return phone;
        }
        return phone.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2");
    }

    /**
     * 身份证号脱敏
     * 格式：1101**********1234
     */
    public static String desensitizeIdCard(String idCard) {
        if (isBlank(idCard)) {
            return idCard;
        }
        int length = idCard.length();
        if (length <= 6) {
            return idCard;
        }
        if (length <= 10) {
            return idCard.substring(0, 4) + "****" + idCard.substring(length - 4);
        }
        return idCard.substring(0, 4) + "**********" + idCard.substring(length - 4);
    }

    /**
     * 邮箱脱敏
     * 格式：t***@example.com
     */
    public static String desensitizeEmail(String email) {
        if (isBlank(email) || !email.contains("@")) {
            return email;
        }
        String[] parts = email.split("@");
        String username = parts[0];
        String domain = parts[1];

        if (username.length() <= 1) {
            return username + "***@" + domain;
        }
        if (username.length() == 2) {
            return username.charAt(0) + "***@" + domain;
        }
        return username.substring(0, 1) + "***" + username.substring(username.length() - 1) + "@" + domain;
    }

    /**
     * 姓名脱敏
     * 格式：张**明
     */
    public static String desensitizeName(String name) {
        if (isBlank(name)) {
            return name;
        }
        int length = name.length();
        if (length == 1) {
            return name;
        }
        if (length == 2) {
            return name.charAt(0) + "*";
        }
        if (length == 3) {
            return name.charAt(0) + "**" + name.charAt(2);
        }
        return name.charAt(0) + "**" + name.substring(length - 1);
    }

    /**
     * 截断字符串到指定长度
     */
    public static String truncate(String str, int maxLength) {
        if (str == null) {
            return null;
        }
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength);
    }

    /**
     * 检查字符串是否为空或空白
     */
    public static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * 检查字符串是否不为空且不是空白
     */
    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }

    /**
     * 如果字符串为空或空白，则返回默认值
     */
    public static String defaultIfBlank(String str, String defaultValue) {
        return isBlank(str) ? defaultValue : str;
    }

    /**
     * 替换非法控制字符
     */
    public static String replaceInvalidChars(String str, String replacement) {
        if (str == null) {
            return null;
        }
        // 替换所有控制字符（除了换行、回车、制表符）
        return str.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", replacement);
    }
}
