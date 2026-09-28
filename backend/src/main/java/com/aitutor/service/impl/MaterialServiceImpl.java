package com.aitutor.service.impl;

import com.aitutor.common.BizException;
import com.aitutor.config.AppProperties;
import com.aitutor.entity.LessonMaterial;
import com.aitutor.entity.LessonMaterialVersion;
import com.aitutor.mapper.LessonMaterialMapper;
import com.aitutor.mapper.LessonMaterialVersionMapper;
import com.aitutor.service.MaterialService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MaterialServiceImpl implements MaterialService {

    private final LessonMaterialMapper materialMapper;
    private final LessonMaterialVersionMapper versionMapper;
    private final AppProperties appProperties;

    @Override
    public List<LessonMaterial> listByPack(Long packId, boolean teacher) {
        QueryWrapper<LessonMaterial> qw = new QueryWrapper<LessonMaterial>()
                .eq("lesson_pack_id", packId)
                .orderByAsc("sort_index").orderByAsc("id");
        if (!teacher) {
            qw.eq("is_open_to_student", 1);
        }
        return materialMapper.selectList(qw);
    }

    @Override
    public LessonMaterial upload(Long packId, LessonMaterial material, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(400, "请选择要上传的文件");
        }
        String filename = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String ext = "";
        int dot = filename.lastIndexOf('.');
        if (dot >= 0) ext = filename.substring(dot);

        String dateDir = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String storedName = UUID.randomUUID().toString().replace("-", "") + ext;
        Path dir = Paths.get(appProperties.getUploadDir(), dateDir);
        try {
            Files.createDirectories(dir);
            file.transferTo(dir.resolve(storedName).toAbsolutePath());
        } catch (IOException e) {
            log.error("文件保存失败", e);
            throw new BizException(500, "文件保存失败");
        }

        material.setLessonPackId(packId);
        material.setFilePath("/uploads/" + dateDir + "/" + storedName);
        material.setFileType(ext.replace(".", ""));
        material.setFileSize(file.getSize());
        if (material.getSortIndex() == null) material.setSortIndex(0);
        if (material.getIsOpenToStudent() == null) material.setIsOpenToStudent(1);
        if (material.getAllowDownload() == null) material.setAllowDownload(1);
        if (material.getVersion() == null) material.setVersion(1);
        if (material.getStatus() == null) material.setStatus("ACTIVE");
        materialMapper.insert(material);
        return material;
    }

    @Override
    public LessonMaterial update(Long id, LessonMaterial material) {
        LessonMaterial exist = materialMapper.selectById(id);
        if (exist == null) throw new BizException(404, "材料不存在");

        // 保存历史版本
        int newVersion = (exist.getVersion() == null ? 1 : exist.getVersion()) + 1;
        LessonMaterialVersion v = new LessonMaterialVersion();
        v.setMaterialId(exist.getId());
        v.setVersion(exist.getVersion() == null ? 1 : exist.getVersion());
        v.setTitle(exist.getTitle());
        v.setDescription(exist.getDescription());
        v.setFilePath(exist.getFilePath());
        v.setFileType(exist.getFileType());
        v.setFileSize(exist.getFileSize());
        versionMapper.insert(v);

        material.setId(id);
        material.setLessonPackId(exist.getLessonPackId());
        if (material.getFilePath() == null) material.setFilePath(exist.getFilePath());
        if (material.getFileType() == null) material.setFileType(exist.getFileType());
        if (material.getFileSize() == null) material.setFileSize(exist.getFileSize());
        if (material.getVersion() == null) material.setVersion(newVersion);
        if (material.getStatus() == null) material.setStatus(exist.getStatus());
        materialMapper.updateById(material);
        return materialMapper.selectById(id);
    }

    @Override
    public void delete(Long id) {
        materialMapper.deleteById(id);
        // 同时删除版本历史
        versionMapper.delete(new QueryWrapper<LessonMaterialVersion>().eq("material_id", id));
    }

    @Override
    public LessonMaterial addText(Long packId, LessonMaterial material) {
        material.setLessonPackId(packId);
        if (material.getSortIndex() == null) material.setSortIndex(0);
        if (material.getIsOpenToStudent() == null) material.setIsOpenToStudent(1);
        if (material.getAllowDownload() == null) material.setAllowDownload(1);
        if (material.getVersion() == null) material.setVersion(1);
        if (material.getStatus() == null) material.setStatus("ACTIVE");
        materialMapper.insert(material);
        return material;
    }

    @Override
    public void sort(List<Map<String, Object>> items) {
        for (Map<String, Object> item : items) {
            Long id = Long.valueOf(String.valueOf(item.get("id")));
            Integer sortIndex = Integer.valueOf(String.valueOf(item.get("sortIndex")));
            LessonMaterial m = materialMapper.selectById(id);
            if (m != null) {
                m.setSortIndex(sortIndex);
                materialMapper.updateById(m);
            }
        }
    }

    @Override
    public List<LessonMaterialVersion> versions(Long materialId) {
        return versionMapper.selectList(
                new QueryWrapper<LessonMaterialVersion>().eq("material_id", materialId).orderByDesc("version"));
    }

    @Override
    public LessonMaterial restoreVersion(Long materialId, Long versionId) {
        LessonMaterialVersion v = versionMapper.selectById(versionId);
        if (v == null || !v.getMaterialId().equals(materialId)) {
            throw new BizException(404, "版本不存在");
        }
        LessonMaterial m = materialMapper.selectById(materialId);
        if (m == null) throw new BizException(404, "材料不存在");
        // 恢复前也保存当前为历史版本
        update(materialId, m);
        m.setTitle(v.getTitle());
        m.setDescription(v.getDescription());
        m.setFilePath(v.getFilePath());
        m.setFileType(v.getFileType());
        m.setFileSize(v.getFileSize());
        m.setVersion((m.getVersion() == null ? 1 : m.getVersion()) + 1);
        materialMapper.updateById(m);
        return materialMapper.selectById(materialId);
    }

    @Override
    public List<LessonMaterial> preview(Long packId) {
        return listByPack(packId, false);
    }
}
