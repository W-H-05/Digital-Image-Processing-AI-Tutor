package com.aitutor.controller;

import com.aitutor.common.R;
import com.aitutor.config.AppProperties;
import com.aitutor.dto.CoursewareDTO;
import com.aitutor.entity.Courseware;
import com.aitutor.entity.KnowledgePoint;
import com.aitutor.mapper.CoursewareMapper;
import com.aitutor.mapper.KnowledgePointMapper;
import com.aitutor.security.AuthUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CoursewareController {

    private final CoursewareMapper coursewareMapper;
    private final KnowledgePointMapper knowledgePointMapper;
    private final AppProperties appProperties;

    @GetMapping("/lesson-packs/{packId}/coursewares")
    public R<List<Courseware>> list(@PathVariable Long packId) {
        AuthUtil.currentUserId();
        return R.ok(coursewareMapper.selectList(
                new QueryWrapper<Courseware>().eq("lesson_pack_id", packId).orderByAsc("id")));
    }

    @GetMapping("/lesson-packs/{packId}/knowledge-points")
    public R<List<KnowledgePoint>> knowledgePoints(@PathVariable Long packId) {
        AuthUtil.currentUserId();
        return R.ok(knowledgePointMapper.selectList(
                new QueryWrapper<KnowledgePoint>().eq("lesson_pack_id", packId).orderByAsc("id")));
    }

    @PostMapping("/teacher/coursewares/{packId}")
    public R<Courseware> create(@PathVariable Long packId, @RequestBody CoursewareDTO dto) {
        AuthUtil.requireTeacher();
        Courseware c = new Courseware();
        BeanUtils.copyProperties(dto, c);
        c.setLessonPackId(packId);
        coursewareMapper.insert(c);
        return R.ok(c);
    }

    @PutMapping("/teacher/coursewares/{id}")
    public R<Courseware> update(@PathVariable Long id, @RequestBody CoursewareDTO dto) {
        AuthUtil.requireTeacher();
        Courseware c = coursewareMapper.selectById(id);
        if (c == null) return R.fail(404, "课件不存在");
        BeanUtils.copyProperties(dto, c, "id", "lessonPackId");
        coursewareMapper.updateById(c);
        return R.ok(c);
    }

    @DeleteMapping("/teacher/coursewares/{id}")
    public R<Void> delete(@PathVariable Long id) {
        AuthUtil.requireTeacher();
        coursewareMapper.deleteById(id);
        return R.ok();
    }

    /** 上传课件示例图（教师端），保存到 resource_path */
    @PostMapping("/teacher/coursewares/{id}/sample-image")
    public R<Courseware> uploadSampleImage(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        AuthUtil.requireTeacher();
        Courseware cw = coursewareMapper.selectById(id);
        if (cw == null) return R.fail(404, "课件不存在");
        if (file == null || file.isEmpty()) return R.fail(400, "请选择图片");
        try {
            String original = file.getOriginalFilename() == null ? "image" : file.getOriginalFilename();
            String ext = "";
            int dot = original.lastIndexOf('.');
            if (dot >= 0) ext = original.substring(dot);
            String dateDir = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String stored = UUID.randomUUID().toString().replace("-", "") + ext;
            Path dir = Paths.get(appProperties.getUploadDir(), "sample", dateDir);
            Files.createDirectories(dir);
            file.transferTo(dir.resolve(stored).toAbsolutePath());
            cw.setResourcePath("/uploads/sample/" + dateDir + "/" + stored);
            coursewareMapper.updateById(cw);
            return R.ok(cw);
        } catch (Exception e) {
            log.error("示例图上传失败", e);
            return R.fail(500, "示例图上传失败");
        }
    }
}
