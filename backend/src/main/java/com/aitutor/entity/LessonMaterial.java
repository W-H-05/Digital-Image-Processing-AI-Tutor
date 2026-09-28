package com.aitutor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("lesson_material")
public class LessonMaterial {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long lessonPackId;
    private String materialType;
    private String title;
    private String description;
    private String filePath;
    private String fileType;
    private Long fileSize;
    private Integer sortIndex;
    private String groupName;
    private Integer isOpenToStudent;
    private Integer allowDownload;
    private Integer version;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
