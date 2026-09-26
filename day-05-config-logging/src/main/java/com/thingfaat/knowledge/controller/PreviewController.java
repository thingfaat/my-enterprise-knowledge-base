package com.thingfaat.knowledge.controller;

import com.thingfaat.knowledge.dto.PreviewResponse;
import com.thingfaat.knowledge.service.PreviewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * 复习查询参数绑定与构造器注入；请求日志与业务日志共享同一个 ID。
 */
@RestController
@RequestMapping("/api/previews")
public class PreviewController {
    private static final Logger log = LoggerFactory.getLogger(PreviewController.class);
    private final PreviewService previewService;

    public PreviewController(PreviewService previewService) {
        this.previewService = previewService;
    }

    @GetMapping
    public PreviewResponse preview(@RequestParam("text") String text) {
        // 本地请求关联示例，不是分布式追踪系统，也不信任客户端提供的日志 ID。
        String requestId = UUID.randomUUID().toString();
        log.info("preview received requestId={}", requestId);
        return previewService.preview(text, requestId);
    }
}
