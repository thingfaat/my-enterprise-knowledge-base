package com.thingfaat.knowledge.service;


import com.thingfaat.knowledge.dto.DocumentResponse;
import com.thingfaat.knowledge.dto.DocumentWriteRequest;
import com.thingfaat.knowledge.entity.DocumentEntity;
import com.thingfaat.knowledge.mapper.DocumentMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * 组织输入检查、Mapper 调用与结果转换；不再持有 Map 或 AtomicLong。
 */
@Service
public class DocumentService {
    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);
    private final DocumentMapper documentMapper;

    public DocumentService(DocumentMapper documentMapper) {
        this.documentMapper = documentMapper;
    }

    /**
     * 数据库生成 ID 并回填；insert 的返回值不是 ID。
     */
    public DocumentResponse create(DocumentWriteRequest request) {
        DocumentEntity entity = prepare(request);
        if (documentMapper.insert(entity) != 1 || entity.getId() == null) {
            throw new IllegalStateException("Insert did not return expected row/key");
        }
        log.info("document created id={}", entity.getId());
        return toResponse(entity);
    }

    /**
     * 单行查询无结果时，MyBatis 返回 null。
     */
    public DocumentResponse detail(Long id) {
        DocumentEntity entity = documentMapper.findById(id);
        if (entity == null) {
            throw notFound();
        }
        return toResponse(entity);
    }

    /**
     * 仅实现标题包含查询，排序和最多 100 条限制位于 SQL。
     */
    public List<DocumentResponse> search(String keyword) {
        return documentMapper.search(keyword.strip()).stream().map(this::toResponse).toList();
    }

    /**
     * PUT 整体替换标题/正文；缺正文表示替换为空字符串，不是“保持原值”。
     */
    public void update(Long id, DocumentWriteRequest request) {
        DocumentEntity entity = prepare(request);
        entity.setId(id);
        // 依赖本课 useAffectedRows=false 的匹配行数语义；同值更新仍应成功。
        if (documentMapper.update(entity) == 0) {
            throw notFound();
        }
        log.info("document updated id={}", id);
    }

    /**
     * 物理删除只作用于指定 ID；不先查再删，直接判断语句结果。
     */
    public void delete(Long id) {
        if (documentMapper.deleteById(id) == 0) {
            throw notFound();
        }
        log.info("document deleted id={}", id);
    }

    private DocumentEntity prepare(DocumentWriteRequest request) {
        String title = request.title() == null ? "" : request.title().strip();
        String content = request.content() == null ? "" : request.content();
        // 用码点数限制输入，正文保留空白；详细注解校验留到 Day 07。
        if (title.isBlank() || title.codePointCount(0, title.length()) > 100
                || content.codePointCount(0, content.length()) > 20000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid title or content length");
        }
        DocumentEntity entity = new DocumentEntity();
        entity.setTitle(title);
        entity.setContent(content);
        return entity;
    }

    private DocumentResponse toResponse(DocumentEntity entity) {
        return new DocumentResponse(entity.getId(), entity.getTitle(), entity.getContent());
    }

    private ResponseStatusException notFound() {
        // 暂保留上一课 HTTP 异常写法；Day 07 再统一拆分异常边界。
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found");
    }
}
