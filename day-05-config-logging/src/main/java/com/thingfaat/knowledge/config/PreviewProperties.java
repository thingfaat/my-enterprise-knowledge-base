package com.thingfaat.knowledge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * app.preview.max-length 绑定到 maxLength；单构造器 record 使用构造器绑定。
 * 注册由启动类 @EnableConfigurationProperties 完成，不再给本类加 @Component。
 */
@ConfigurationProperties(prefix = "app.preview")
public record PreviewProperties(String label, int maxLength) {
    public PreviewProperties {
        // 提前拒绝不可用配置：比收到请求后才报错更容易定位。
        // 完整 Bean Validation 留到 Day 07，这里使用 Java 构造器检查。
        if (label == null || !label.matches("[A-Za-z0-9_-]{1,32}")) {
            throw new IllegalArgumentException("app.preview.label must be a short alphanumeric label");
        }
        if (maxLength < 1 || maxLength > 100) {
            throw new IllegalArgumentException("app.preview.max-length must be between 1 and 100");
        }
    }
}
