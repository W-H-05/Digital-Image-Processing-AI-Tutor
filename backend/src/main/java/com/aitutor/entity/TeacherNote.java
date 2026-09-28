package com.aitutor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("teacher_note")
public class TeacherNote {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long teacherId;
    private Long lessonPackId;
    private String noteContent;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
