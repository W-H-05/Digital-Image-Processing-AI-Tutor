package com.aitutor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("courseware")
public class Courseware {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long lessonPackId;
    private String name;
    private String type;
    private String configJson;
    private String resourcePath;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
