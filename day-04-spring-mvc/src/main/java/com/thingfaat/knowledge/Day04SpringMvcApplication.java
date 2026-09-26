package com.thingfaat.knowledge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 启动当天独立应用，扫描本包及其子包中的 Controller 和 Service。
 */
@SpringBootApplication
public class Day04SpringMvcApplication {
    public static void main(String[] args) {
        SpringApplication.run(Day04SpringMvcApplication.class, args);
    }
}
