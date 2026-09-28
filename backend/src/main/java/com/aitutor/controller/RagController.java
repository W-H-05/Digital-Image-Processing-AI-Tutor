package com.aitutor.controller;

import com.aitutor.ai.RagService;
import com.aitutor.common.R;
import com.aitutor.entity.RagDocument;
import com.aitutor.mapper.RagDocumentMapper;
import com.aitutor.security.AuthUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * RAG 知识库管理（教师端）
 */
@RestController
@RequestMapping("/api/teacher/rag")
@RequiredArgsConstructor
public class RagController {

    private final RagDocumentMapper ragDocumentMapper;
    private final RagService ragService;

    @GetMapping
    public R<List<RagDocument>> list(@RequestParam(value = "category", required = false) String category) {
        AuthUtil.requireTeacher();
        QueryWrapper<RagDocument> qw = new QueryWrapper<>();
        qw.eq(category != null && !category.isBlank(), "category", category).orderByAsc("id");
        return R.ok(ragDocumentMapper.selectList(qw));
    }

    @PostMapping
    public R<RagDocument> add(@RequestBody RagDocument doc) {
        AuthUtil.requireTeacher();
        if (doc.getTitle() == null || doc.getTitle().isBlank()) {
            return R.fail(400, "标题不能为空");
        }
        if (doc.getContent() == null || doc.getContent().isBlank()) {
            return R.fail(400, "内容不能为空");
        }
        ragDocumentMapper.insert(doc);
        ragService.reload();
        return R.ok(doc);
    }

    @PutMapping("/{id}")
    public R<RagDocument> update(@PathVariable Long id, @RequestBody RagDocument doc) {
        AuthUtil.requireTeacher();
        RagDocument exist = ragDocumentMapper.selectById(id);
        if (exist == null) return R.fail(404, "文档不存在");
        doc.setId(id);
        ragDocumentMapper.updateById(doc);
        ragService.reload();
        return R.ok(ragDocumentMapper.selectById(id));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        AuthUtil.requireTeacher();
        ragDocumentMapper.deleteById(id);
        ragService.reload();
        return R.ok();
    }

    @GetMapping("/stats")
    public R<Map<String, Object>> stats() {
        AuthUtil.requireTeacher();
        return R.ok(Map.of("total", ragService.count()));
    }
}
