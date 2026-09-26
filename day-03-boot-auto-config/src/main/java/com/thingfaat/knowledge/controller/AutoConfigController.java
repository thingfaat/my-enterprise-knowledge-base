package com.thingfaat.knowledge.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 由组件扫描发现的业务 Bean；HTTP 映射还需要 MVC 基础设施。
 */
@RestController
public class AutoConfigController {
    /**
     * 无请求参数和存储操作，仅用固定文本验证 Web 请求链路。
     */
    @GetMapping(value = "/api/auto-config", produces = MediaType.TEXT_PLAIN_VALUE)
    public String inspect() {
        return "Web auto-configuration is active!";
    }
}
