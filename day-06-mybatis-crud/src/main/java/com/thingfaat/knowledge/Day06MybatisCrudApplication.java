package com.thingfaat.knowledge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 当天独立应用；Starter 在扫描范围内发现带 @Mapper 的接口。
 */
@SpringBootApplication
public class Day06MybatisCrudApplication {
    public static void main(String[] args) {
        SpringApplication.run(Day06MybatisCrudApplication.class, args);
    }
}
