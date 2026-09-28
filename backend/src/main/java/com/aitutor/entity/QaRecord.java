package com.aitutor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("qa_record")
public class QaRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long lessonPackId;
    private Long materialId;
    private String question;
    private String answer;
    private String sourceRefs;
    private Integer followUpCount;
    private Integer isSyncedToTeacher;
    private Integer hasImage;
    private LocalDateTime createTime;
}
