package com.thingfaat.knowledge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Day 02 独立应用入口。
 * controller 与 service 都在此包下，使默认组件扫描覆盖两者。
 */
@SpringBootApplication
public class Day02SpringDiApplication {
    /**
     * 启动本课的容器与 Web 服务，保留命令行参数传递。
     */
    public static void main(String[] args) {
        SpringApplication.run(Day02SpringDiApplication.class, args);
    }
}
