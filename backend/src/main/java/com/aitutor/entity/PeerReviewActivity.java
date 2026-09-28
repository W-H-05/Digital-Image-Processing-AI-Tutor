package com.aitutor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("peer_review_activity")
public class PeerReviewActivity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String mode;
    private Long rubricId;
    private Long lessonPackId;
    private Integer durationSec;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createTime;
}
