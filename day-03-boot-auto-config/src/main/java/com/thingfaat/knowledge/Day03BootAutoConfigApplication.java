package com.thingfaat.knowledge;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

/**
 * 启动当天独立实验，并输出容器证据；不依赖 Servlet 或 MVC 的类型。
 */
@SpringBootApplication
public class Day03BootAutoConfigApplication {
    public static void main(String[] args) {
        SpringApplication.run(Day03BootAutoConfigApplication.class, args);
    }

    /**
     * @Bean 将返回对象交给容器管理，context 参数由容器注入。
     * Boot 在容器刷新后调用 ApplicationRunner，适合观察启动结果。
     * containsBean 只检查名称是否存在，不等于验证 HTTP 请求已经成功。
     */
    @Bean
    public ApplicationRunner inspectContext(ApplicationContext context) {
        return args -> {
            System.out.println("context=" + context.getClass().getSimpleName());
            System.out.println("autoConfigController=" + context.containsBean("autoConfigController"));
            System.out.println("dispatcherServlet=" + context.containsBean("dispatcherServlet"));
        };
    }
}
