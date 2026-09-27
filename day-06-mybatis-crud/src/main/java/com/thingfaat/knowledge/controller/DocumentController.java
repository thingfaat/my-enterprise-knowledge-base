package com.thingfaat.knowledge.controller;

import com.thingfaat.knowledge.dto.DocumentResponse;
import com.thingfaat.knowledge.dto.DocumentWriteRequest;
import com.thingfaat.knowledge.service.DocumentService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/**
 * HTTP 适配层，不在 Controller 写 SQL；复习 Day 04 参数绑定。
 */
@RestController
@RequestMapping(value = "/api/documents", produces = MediaType.APPLICATION_JSON_VALUE)
public class DocumentController {
    private final DocumentService service;

    public DocumentController(DocumentService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DocumentResponse> create(@RequestBody DocumentWriteRequest request) {
        DocumentResponse response = service.create(request);
        // Location 包含正确的路径分隔符，可直接用于后续详情查询。
        return ResponseEntity.created(URI.create("/api/documents/" + response.id())).body(response);
    }

    @GetMapping("/{id}")
    public DocumentResponse detail(@PathVariable("id") Long id) {
        return service.detail(id);
    }

    @GetMapping
    public List<DocumentResponse> search(@RequestParam(name = "keyword", defaultValue = "") String keyword) {
        return service.search(keyword);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> update(@PathVariable("id") Long id, @RequestBody DocumentWriteRequest request) {
        service.update(id, request);
        // 写入成功返回 204，无响应正文；用 GET 再核对数据库结果。
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
