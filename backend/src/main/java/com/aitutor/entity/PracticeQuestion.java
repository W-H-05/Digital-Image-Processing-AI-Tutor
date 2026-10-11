package com.aitutor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("practice_question")
public class PracticeQuestion {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long lessonPackId;
    private Integer questionNo;
    private String stem;
    private String optionsJson;
    private String correctKeys;
    private Integer isMultiple;
    private Integer sortIndex;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
