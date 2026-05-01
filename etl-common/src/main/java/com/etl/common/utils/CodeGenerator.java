package com.etl.common.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 编码生成工具类
 */
public class CodeGenerator {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private CodeGenerator() {}

    /**
     * 生成执行编号
     */
    public static String generateExecutionNo() {
        String dateTime = LocalDateTime.now().format(DATE_TIME_FORMATTER);
        int random = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "EXEC" + dateTime + random;
    }

    /**
     * 生成任务编号
     */
    public static String generateTaskNo() {
        String dateTime = LocalDateTime.now().format(DATE_TIME_FORMATTER);
        int random = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "TASK" + dateTime + random;
    }

    /**
     * 生成Kafka Topic名称
     */
    public static String generateKafkaTopic(String prefix, String datasourceId, String tableName) {
        return prefix + datasourceId + "-" + tableName;
    }
}
