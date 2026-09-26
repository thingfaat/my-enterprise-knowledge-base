package com.thingfaat.knowledge.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 验证 HTTP 请求能进入当天模块的应用。
 * 没有数据库操作，先用固定响应观察完整的请求与响应过程。
 */
@RestController
@RequestMapping("/api")
public class HelloController {
    /**
     * 类路径 /api 与方法路径 /hello 组合为 GET /api/hello。
     * 返回字符串直接写入响应体，并明确使用纯文本响应类型。
     *
     * @return 本课用于验收的问候语
     */
    @GetMapping(value = "/hello", produces = MediaType.TEXT_PLAIN_VALUE)
    public String hello() {
        return "Hello, day-01-spring-boot-hello";
    }
}
