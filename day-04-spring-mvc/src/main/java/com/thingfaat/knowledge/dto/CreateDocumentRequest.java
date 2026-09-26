package com.thingfaat.knowledge.dto;

/**
 * 接收 JSON 请求体，只允许调用方提供标题和正文，不让调用方决定服务端 ID。
 * Java 17 record 自动提供构造器和 title()/content() 访问器；这里不使用 Lombok。
 * JSON 解析成功不代表字段合法，标题的最小检查在 Controller 中完成。
 */
public record CreateDocumentRequest(String title, String content) {
}
