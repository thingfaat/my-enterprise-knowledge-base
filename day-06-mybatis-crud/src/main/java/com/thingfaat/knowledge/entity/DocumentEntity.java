package com.thingfaat.knowledge.entity;

/**
 * 数据库行的 Java 表示。可变 ID 用于接收数据库生成的主键回填。
 * 显式提供无参构造器和访问器，避免此时引入 Lombok 或构造器映射知识。
 */
public class DocumentEntity {
    private Long id;
    private String title;
    private String content;

    public DocumentEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
