package com.aitutor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("confusion_cluster")
public class ConfusionCluster {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long lessonPackId;
    private Long knowledgePointId;
    private String clusterName;
    private Integer questionCount;
    private Integer studentCount;
    private String sampleQuestions;
    private LocalDateTime updateTime;
}
