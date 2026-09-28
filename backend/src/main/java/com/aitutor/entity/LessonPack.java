package com.aitutor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("lesson_pack")
public class LessonPack {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long courseId;
    private String lessonNo;
    private String hours;
    private String title;
    private String chapter;
    private String objectives;
    private String contentSummary;
    private String keyPoints;
    private String difficultPoints;
    private String ideologicalNotes;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
