package com.zhixue.ai;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 智学AI学习测评系统 启动类
 */
@SpringBootApplication
@MapperScan("com.zhixue.ai.module.**.mapper")
@EnableAsync
public class ZhixueAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZhixueAiApplication.class, args);
        System.out.println("" +
                "\n========================================" +
                "\n  智学AI学习测评系统 启动成功" +
                "\n  访问地址: http://localhost:8080" +
                "\n========================================");
    }
}
