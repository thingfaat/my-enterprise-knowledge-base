package com.thingfaat.knowledge.dto;

/**
 * 本机实验的响应；requestId 用于关联日志，label/maxLength 仅是无敏感信息的教学配置。
 */
public record PreviewResponse(String requestId, String label, int maxLength, String preview) {
}
