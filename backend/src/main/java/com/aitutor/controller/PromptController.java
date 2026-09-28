package com.aitutor.controller;

import com.aitutor.common.R;
import com.aitutor.entity.PromptTemplate;
import com.aitutor.mapper.PromptTemplateMapper;
import com.aitutor.security.AuthUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Prompt 模板管理（教师端）
 */
@RestController
@RequestMapping("/api/teacher/prompts")
@RequiredArgsConstructor
public class PromptController {

    private final PromptTemplateMapper promptTemplateMapper;

    @GetMapping
    public R<List<PromptTemplate>> list() {
        AuthUtil.requireTeacher();
        return R.ok(promptTemplateMapper.selectList(
                new QueryWrapper<PromptTemplate>().orderByAsc("id")));
    }

    @PostMapping
    public R<PromptTemplate> add(@RequestBody PromptTemplate template) {
        AuthUtil.requireTeacher();
        if (template.getName() == null || template.getName().isBlank()) {
            return R.fail(400, "模板名称不能为空");
        }
        promptTemplateMapper.insert(template);
        return R.ok(template);
    }

    @PutMapping("/{id}")
    public R<PromptTemplate> update(@PathVariable Long id, @RequestBody PromptTemplate template) {
        AuthUtil.requireTeacher();
        PromptTemplate exist = promptTemplateMapper.selectById(id);
        if (exist == null) return R.fail(404, "模板不存在");
        template.setId(id);
        promptTemplateMapper.updateById(template);
        return R.ok(promptTemplateMapper.selectById(id));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        AuthUtil.requireTeacher();
        promptTemplateMapper.deleteById(id);
        return R.ok();
    }
}
