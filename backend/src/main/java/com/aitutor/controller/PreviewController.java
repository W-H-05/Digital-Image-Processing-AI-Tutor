package com.aitutor.controller;

import com.aitutor.common.R;
import com.aitutor.entity.LessonMaterial;
import com.aitutor.mapper.LessonMaterialMapper;
import com.aitutor.security.AuthUtil;
import com.aitutor.service.PreviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 文档在线预览：PPT/PPTX/PDF 转图片序列，前端翻页查看
 */
@RestController
@RequestMapping("/api/preview")
@RequiredArgsConstructor
public class PreviewController {

    private final PreviewService previewService;
    private final LessonMaterialMapper materialMapper;

    /**
     * 按材料 ID 预览（教师/学生均可访问，但学生只能预览开放材料）
     */
    @GetMapping("/material/{materialId}")
    public R<Map<String, Object>> previewMaterial(@PathVariable Long materialId) {
        Long userId = AuthUtil.currentUserId();
        LessonMaterial m = materialMapper.selectById(materialId);
        if (m == null) {
            return R.fail(404, "材料不存在");
        }
        // 学生只能预览开放材料
        if (!AuthUtil.isTeacher() && (m.getIsOpenToStudent() == null || m.getIsOpenToStudent() != 1)) {
            return R.fail(403, "该材料未向学生开放");
        }
        if (!previewService.support(m.getFileType())) {
            return R.fail(400, "该文件类型不支持在线预览，请下载查看");
        }
        List<String> images = previewService.toImages(m.getFilePath());
        return R.ok(Map.of("images", images, "title", m.getTitle(), "fileType", m.getFileType()));
    }
}
