package com.thingfaat.knowledge.controller;

import com.thingfaat.knowledge.service.GreetingService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 接收 HTTP 参数并委派业务处理，不在控制器中自行创建 Service。
 */
@RestController
@RequestMapping("/api")
public class HelloController {

    // 必需依赖由构造器提供，final 防止创建后被替换。
    private final GreetingService greetingService;

    /**
     * Spring 创建控制器时注入已注册的 GreetingService。
     * 本类只有一个构造器，无需额外标注 @Autowired。
     */
    public HelloController(GreetingService greetingService) {
        this.greetingService = greetingService;
    }

    /**
     * 接收可选 name；未提供时为 null，由 Service 决定默认问候对象。
     *
     * @param name 请求中的名字
     * @return Service 生成的纯文本问候语
     */
    @GetMapping(value = "/hello", produces = MediaType.TEXT_PLAIN_VALUE)
    public String hello(@RequestParam(name = "name", required = false) String name) {
        return greetingService.greet(name);
    }
}
