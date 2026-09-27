package com.thingfaat.knowledge.dto;

/**
 * HTTP 输出，与持久化 Entity 分开；以后表增加内部字段不自动暴露给客户端。
 */
public record DocumentResponse(Long id, String title, String content) {
}
