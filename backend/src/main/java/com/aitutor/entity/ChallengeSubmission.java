package com.aitutor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("challenge_submission")
public class ChallengeSubmission {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long lessonPackId;
    private Long coursewareId;
    private String paramsJson;
    private String screenshotPath;
    private String conclusion;
    private Integer score;
    private LocalDateTime createTime;
}
