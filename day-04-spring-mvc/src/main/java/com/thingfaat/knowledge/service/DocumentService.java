package com.thingfaat.knowledge.service;

import com.thingfaat.knowledge.dto.CreateDocumentRequest;
import com.thingfaat.knowledge.dto.DocumentResponse;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 当天的内存存储：复习 Service 和构造器注入，不依赖数据库或前一天的应用。
 * 多请求共享这个单例；Map 和序号使用并发安全类型，但不提供数据库事务能力。
 * 重启丢失全部数据，不能直接作为正式项目的持久化设计。
 */
@Service
public class DocumentService {
    private final Map<Long, DocumentResponse> documents = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    /**
     * 接收已经检查过标题的输入，生成服务端 ID，保存并返回文档。
     */
    public DocumentResponse create(CreateDocumentRequest request) {
        long id = sequence.incrementAndGet();
        DocumentResponse document = new DocumentResponse(
                id,
                request.title().strip(),
                request.content() == null ? "" : request.content().strip()
        );
        documents.put(id, document);
        return document;
    }

    /**
     * 不存在时返回空 Optional，让 HTTP 层决定对应状态码。
     */
    public Optional<DocumentResponse> findById(Long id) {
        return Optional.ofNullable(documents.get(id));
    }

    /**
     * 标题区分大小写的包含查询；空关键词返回全部，按 ID 排序方便验收。
     */
    public List<DocumentResponse> search(String keyword) {
        String normalized = keyword.strip();
        return documents.values().stream()
                .filter(document-> document.title().contains(normalized))
                .sorted(Comparator.comparing(DocumentResponse::id))
                .toList();
    }
}
