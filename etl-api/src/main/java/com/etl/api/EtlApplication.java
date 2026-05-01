package com.etl.api;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * ETL数据同步调度系统启动类
 */
@SpringBootApplication(scanBasePackages = "com.etl")
@MapperScan("com.etl.**.mapper")
@EnableScheduling
public class EtlApplication {

    public static void main(String[] args) {
        SpringApplication.run(EtlApplication.class, args);
        System.out.println("======================================");
        System.out.println("  ETL数据同步调度系统启动成功!");
        System.out.println("  API文档: http://localhost:8080/doc.html");
        System.out.println("======================================");
    }
}
