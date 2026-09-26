package com.thingfaat.knowledge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Day 01 独立应用的启动入口。
 * 放在根包，默认组件扫描可以发现 controller 子包中的控制器。
 */
@SpringBootApplication
public class Day01SpringBootHelloApplication {
    /**
     * 由 JVM 进入，再交给 Spring Boot 初始化上下文和 Web 服务器。
     * args 用于将命令行参数传给应用。
     */
    public static void main(String[] args) {
        SpringApplication.run(Day01SpringBootHelloApplication.class, args);
    }
}
