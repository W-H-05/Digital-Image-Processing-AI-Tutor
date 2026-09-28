package com.aitutor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("homework_submission")
public class HomeworkSubmission {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long homeworkId;
    private Long userId;
    private String answersJson;
    private Integer score;
    private Integer autoScored;
    private LocalDateTime submitTime;
}
