package com.aitutor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("homework")
public class Homework {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long lessonPackId;
    private String title;
    private String description;
    private String questionsJson;
    private LocalDateTime deadline;
    private Integer totalScore;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
