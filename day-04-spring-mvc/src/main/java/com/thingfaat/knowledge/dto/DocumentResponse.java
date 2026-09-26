package com.thingfaat.knowledge.dto;

/**
 * 返回给客户端的不可变数据载体，由 Jackson 序列化成 JSON 字段。
 */
public record DocumentResponse(Long id, String title, String content) {
}
