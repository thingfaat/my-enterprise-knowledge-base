package com.thingfaat.knowledge.service;

import org.springframework.stereotype.Service;

/**
 * 集中处理问候语规则：空白使用 World，非空白去掉首尾空格。
 *
 * @Service 让此类在组件扫描时注册为 Bean，供控制器注入。
 */
@Service
public class GreetingService {
    /**
     * 业务方法不依赖 HTTP 对象，可以被不同入口复用。
     *
     * @param name 用户名字，允许 null 或空白
     * @return 格式统一的问候语
     */
    public String greet(String name) {
        // strip：删除所有前导和尾部空白
        String displayName = (name == null || name.isBlank()) ? "world" : name.strip();
        return "Hello, " + displayName + "!";
    }
}
