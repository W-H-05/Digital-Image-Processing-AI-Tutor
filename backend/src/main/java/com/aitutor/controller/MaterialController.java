package com.aitutor.controller;

import com.aitutor.common.R;
import com.aitutor.entity.LessonMaterial;
import com.aitutor.entity.LessonMaterialVersion;
import com.aitutor.security.AuthUtil;
import com.aitutor.service.MaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MaterialController {

    private final MaterialService materialService;

    /** 材料列表（学生只看到开放材料；教师可切换学生视角） */
    @GetMapping("/lesson-packs/{packId}/materials")
    public R<List<LessonMaterial>> list(@PathVariable Long packId) {
        boolean teacher = AuthUtil.effectiveTeacher();
        return R.ok(materialService.listByPack(packId, teacher));
    }

    /** 上传材料（教师） */
    @PostMapping("/teacher/materials/{packId}")
    public R<LessonMaterial> upload(@PathVariable Long packId,
                                    @RequestParam(value = "materialType", required = false) String materialType,
                                    @RequestParam(value = "title", required = false) String title,
                                    @RequestParam(value = "description", required = false) String description,
                                    @RequestParam(value = "groupName", required = false) String groupName,
                                    @RequestParam(value = "isOpenToStudent", required = false) Integer isOpenToStudent,
                                    @RequestParam("file") MultipartFile file) {
        AuthUtil.requireTeacher();
        LessonMaterial material = new LessonMaterial();
        material.setMaterialType(materialType);
        material.setTitle(title);
        material.setDescription(description);
        material.setGroupName(groupName);
        material.setIsOpenToStudent(isOpenToStudent);
        return R.ok(materialService.upload(packId, material, file));
    }

    /** 新增文本材料（重难点/知识点/作业等） */
    @PostMapping("/teacher/materials/{packId}/text")
    public R<LessonMaterial> addText(@PathVariable Long packId, @RequestBody LessonMaterial material) {
        AuthUtil.requireTeacher();
        return R.ok(materialService.addText(packId, material));
    }

    /** 编辑材料 */
    @PutMapping("/teacher/materials/{id}")
    public R<LessonMaterial> update(@PathVariable Long id, @RequestBody LessonMaterial material) {
        AuthUtil.requireTeacher();
        return R.ok(materialService.update(id, material));
    }

    @DeleteMapping("/teacher/materials/{id}")
    public R<Void> delete(@PathVariable Long id) {
        AuthUtil.requireTeacher();
        materialService.delete(id);
        return R.ok();
    }

    /** 拖拽排序 */
    @PostMapping("/teacher/materials/sort")
    public R<Void> sort(@RequestBody Map<String, Object> body) {
        AuthUtil.requireTeacher();
        List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
        materialService.sort(items);
        return R.ok();
    }

    /** 材料版本历史 */
    @GetMapping("/teacher/materials/{id}/versions")
    public R<List<LessonMaterialVersion>> versions(@PathVariable Long id) {
        AuthUtil.requireTeacher();
        return R.ok(materialService.versions(id));
    }

    /** 恢复版本 */
    @PostMapping("/teacher/materials/{id}/restore/{versionId}")
    public R<LessonMaterial> restore(@PathVariable Long id, @PathVariable Long versionId) {
        AuthUtil.requireTeacher();
        return R.ok(materialService.restoreVersion(id, versionId));
    }

    /** 教师预览学生端效果 */
    @GetMapping("/teacher/lesson-packs/{packId}/preview")
    public R<List<LessonMaterial>> preview(@PathVariable Long packId) {
        AuthUtil.requireTeacher();
        return R.ok(materialService.preview(packId));
    }
}
