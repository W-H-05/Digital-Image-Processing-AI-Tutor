package com.aitutor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("lesson_material_version")
public class LessonMaterialVersion {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long materialId;
    private Integer version;
    private String title;
    private String description;
    private String filePath;
    private String fileType;
    private Long fileSize;
    private LocalDateTime createTime;
}
