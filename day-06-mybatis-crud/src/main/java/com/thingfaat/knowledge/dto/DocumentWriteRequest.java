package com.thingfaat.knowledge.dto;

/**
 * 创建/整体更新的输入；ID 来自数据库或 URL，不由 JSON 决定。
 */
public record DocumentWriteRequest(String title, String content) {
}
