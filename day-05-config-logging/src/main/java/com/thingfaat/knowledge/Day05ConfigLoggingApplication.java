package com.thingfaat.knowledge;

import com.thingfaat.knowledge.config.PreviewProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import java.util.Arrays;

/**
 * 启动独立实验，并显式注册强类型配置 Bean。
 */
@SpringBootApplication
@EnableConfigurationProperties(PreviewProperties.class)
public class Day05ConfigLoggingApplication {
    private final static Logger log = LoggerFactory.getLogger(Day05ConfigLoggingApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(Day05ConfigLoggingApplication.class, args);
    }

    /**
     * 只记录明确允许展示的教学配置，不遍历或打印整个 Environment。
     */
    @Bean
    public ApplicationRunner reportConfiguration(Environment environment, PreviewProperties properties) {
        return args -> log.info("config activeProfiles={} label={} maxLength={}",
                Arrays.toString(environment.getActiveProfiles()), properties.label(), properties.maxLength());
    }
}
