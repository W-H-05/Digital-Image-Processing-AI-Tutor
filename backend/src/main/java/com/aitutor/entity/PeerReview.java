package com.aitutor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("peer_review")
public class PeerReview {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long reviewerId;
    private Long revieweeId;
    private Long taskId;
    private String scoresJson;
    private String comments;
    private LocalDateTime createTime;
}
