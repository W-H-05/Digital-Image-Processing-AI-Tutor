package com.aitutor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("learning_stats")
public class LearningStats {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long lessonPackId;
    private Long knowledgePointId;
    private java.math.BigDecimal masteryScore;
    private Integer confusionCount;
    private Integer errorCount;
    private java.math.BigDecimal participationScore;
    private LocalDateTime updateTime;
}
