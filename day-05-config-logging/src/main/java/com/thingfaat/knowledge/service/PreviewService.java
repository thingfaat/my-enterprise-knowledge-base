package com.thingfaat.knowledge.service;

import com.thingfaat.knowledge.config.PreviewProperties;
import com.thingfaat.knowledge.dto.PreviewResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * 根据启动时绑定的长度上限生成预览；不保存正文、不访问数据库。
 */
@Service
public class PreviewService {
    private static final Logger log = LoggerFactory.getLogger(PreviewService.class);
    private final PreviewProperties properties;

    public PreviewService(PreviewProperties properties) {
        this.properties = properties;
    }

    /**
     * requestId 由 Controller 生成；日志只记录 ID、长度和分支，不记录原文。
     */
    public PreviewResponse preview(String text, String requestId) {
        if (text.isBlank()) {
            log.warn("preview rejected requestId={} reason=blank_text", requestId);
            // 本课暂在 Service 使用 HTTP 异常；Day 07 再拆分业务异常与 HTTP 映射。
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Text is required");
        }

        // 按 Unicode 码点截断，避免把 emoji 的 UTF-16 代理对切断。
        // 码点仍不等于用户感知字符，例如组合字符，今天不实现完整字素分割。
        int inputLength = text.codePointCount(0, text.length());
        int outputLength = Math.min(inputLength, properties.maxLength());
        int end = text.offsetByCodePoints(0, outputLength);
        String result = text.substring(0, end);
        log.debug("preview decision requestId={} inputLength={} limit={} truncated={}",
                requestId, inputLength, properties.maxLength(), inputLength > outputLength);
        log.info("preview completed requestId={} outputLength={}", requestId, outputLength);
        return new PreviewResponse(requestId, properties.label(), properties.maxLength(), result);
    }
}
