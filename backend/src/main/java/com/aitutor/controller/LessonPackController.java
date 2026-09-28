package com.aitutor.controller;

import com.aitutor.common.R;
import com.aitutor.dto.LessonPackDTO;
import com.aitutor.entity.LessonPack;
import com.aitutor.security.AuthUtil;
import com.aitutor.service.LessonPackService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class LessonPackController {

    private final LessonPackService lessonPackService;

    /** 学生/教师通用：课次包列表（教师可切换学生视角） */
    @GetMapping("/lesson-packs")
    public R<List<LessonPack>> list() {
        boolean teacher = AuthUtil.effectiveTeacher();
        return R.ok(lessonPackService.list(teacher));
    }

    @GetMapping("/lesson-packs/{id}")
    public R<LessonPack> detail(@PathVariable Long id) {
        return R.ok(lessonPackService.getById(id));
    }

    @PostMapping("/teacher/lesson-packs")
    public R<LessonPack> create(@RequestBody LessonPackDTO dto) {
        AuthUtil.requireTeacher();
        LessonPack pack = new LessonPack();
        BeanUtils.copyProperties(dto, pack);
        return R.ok(lessonPackService.create(pack));
    }

    @PutMapping("/teacher/lesson-packs/{id}")
    public R<LessonPack> update(@PathVariable Long id, @RequestBody LessonPackDTO dto) {
        AuthUtil.requireTeacher();
        LessonPack pack = new LessonPack();
        BeanUtils.copyProperties(dto, pack);
        return R.ok(lessonPackService.update(id, pack));
    }

    @DeleteMapping("/teacher/lesson-packs/{id}")
    public R<Void> delete(@PathVariable Long id) {
        AuthUtil.requireTeacher();
        lessonPackService.delete(id);
        return R.ok();
    }

    @PostMapping("/teacher/lesson-packs/{id}/publish")
    public R<Void> publish(@PathVariable Long id) {
        AuthUtil.requireTeacher();
        lessonPackService.publish(id);
        return R.ok();
    }

    @PostMapping("/teacher/lesson-packs/{id}/offline")
    public R<Void> offline(@PathVariable Long id) {
        AuthUtil.requireTeacher();
        lessonPackService.offline(id);
        return R.ok();
    }

    @PostMapping("/teacher/lesson-packs/{id}/copy")
    public R<LessonPack> copy(@PathVariable Long id) {
        AuthUtil.requireTeacher();
        return R.ok(lessonPackService.copy(id));
    }
}
