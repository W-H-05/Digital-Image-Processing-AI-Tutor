package com.aitutor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("code_help_record")
public class CodeHelpRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long lessonPackId;
    private String errorText;
    private String diagnosis;
    private String suggestion;
    private String sourceRefs;
    private LocalDateTime createTime;
}
