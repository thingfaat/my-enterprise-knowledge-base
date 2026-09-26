package com.thingfaat.knowledge.controller;

import com.thingfaat.knowledge.dto.CreateDocumentRequest;
import com.thingfaat.knowledge.dto.DocumentResponse;
import com.thingfaat.knowledge.service.DocumentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;

/**
 * HTTP 边界：绑定参数、做最小输入检查、委托 Service，并选择响应状态。
 */
@RestController
@RequestMapping(value = "/api/documents", produces = MediaType.APPLICATION_JSON_VALUE)
public class DocumentController {
    private final DocumentService documentService;

    // 复习 Day 02：单构造器无需 @Autowired，由容器注入 Service。
    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    /**
     * 路径中的 id 由 MVC 从字符串转换为 Long；转换失败时不会进入方法体。
     */
    @GetMapping("/{id}")
    public DocumentResponse detail(@PathVariable("id") Long id) {
        return documentService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Document not found"
                ));
    }

    /**
     * 查询参数不写或为空时使用空字符串，从而查询全部文档。
     */
    @GetMapping
    public List<DocumentResponse> search(@RequestParam(name = "keyword", defaultValue = "") String keyword) {
        return documentService.search(keyword);
    }

    /**
     * @RequestBody 让消息转换器把 JSON 读为请求对象，不是读取 URL 查询参数。
     * consumes 限制请求媒体类型；ResponseEntity 控制状态码、Location 和正文。
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DocumentResponse> create(@RequestBody CreateDocumentRequest request) {
        // 今天手动做最小校验，Day 07 再学习 @Valid 和统一异常处理
        if (request.title() == null || request.title().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title is required");
        }
        DocumentResponse document = documentService.create(request);
        return ResponseEntity.created(
                URI.create("/api/documents/" + document.id())
        ).body(document);
    }
}
