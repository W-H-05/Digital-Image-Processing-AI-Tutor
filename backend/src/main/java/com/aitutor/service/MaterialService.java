package com.aitutor.service;

import com.aitutor.entity.LessonMaterial;
import com.aitutor.entity.LessonMaterialVersion;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface MaterialService {

    /** 材料列表：学生只看到开放材料 */
    List<LessonMaterial> listByPack(Long packId, boolean teacher);

    /** 上传材料 */
    LessonMaterial upload(Long packId, LessonMaterial material, MultipartFile file);

    /** 编辑材料（开放控制/排序/标题等），保存历史版本 */
    LessonMaterial update(Long id, LessonMaterial material);

    void delete(Long id);

    /** 新增文本材料（重难点/知识点/作业等，无文件） */
    LessonMaterial addText(Long packId, LessonMaterial material);

    /** 拖拽排序：批量更新 sortIndex */
    void sort(List<Map<String, Object>> items);

    /** 材料版本历史 */
    List<LessonMaterialVersion> versions(Long materialId);

    /** 恢复到某个版本 */
    LessonMaterial restoreVersion(Long materialId, Long versionId);

    /** 教师预览学生端效果：返回该课次包对学生开放的材料 */
    List<LessonMaterial> preview(Long packId);
}
