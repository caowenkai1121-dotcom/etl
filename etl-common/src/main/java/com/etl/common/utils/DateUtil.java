package com.etl.common.utils;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 日期时间工具类
 * 线程安全，使用 DateTimeFormatter 替代 SimpleDateFormat
 */
public class DateUtil {

    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter COMPACT_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private DateUtil() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    /**
     * 格式化 LocalDateTime 为 yyyy-MM-dd HH:mm:ss 字符串
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return DATE_TIME_FORMATTER.format(dateTime);
    }

    /**
     * 格式化 LocalDate 为 yyyy-MM-dd 字符串
     */
    public static String formatDate(LocalDate date) {
        if (date == null) {
            return null;
        }
        return DATE_FORMATTER.format(date);
    }

    /**
     * 解析 yyyy-MM-dd HH:mm:ss 字符串为 LocalDateTime
     */
    public static LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            return null;
        }
        return LocalDateTime.parse(dateTimeStr.trim(), DATE_TIME_FORMATTER);
    }

    /**
     * 解析 yyyy-MM-dd 字符串为 LocalDate
     */
    public static LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        return LocalDate.parse(dateStr.trim(), DATE_FORMATTER);
    }

    /**
     * 获取当前时间的紧凑格式字符串 yyyyMMddHHmmss
     */
    public static String compactNow() {
        return COMPACT_FORMATTER.format(LocalDateTime.now());
    }

    /**
     * 计算两个 LocalDateTime 之间的秒数差
     */
    public static long betweenSeconds(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("开始和结束时间不能为空");
        }
        return Duration.between(start, end).getSeconds();
    }
}
