package com.aitutor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("peer_review_rubric")
public class PeerReviewRubric {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String dimensionsJson;
    private String description;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
