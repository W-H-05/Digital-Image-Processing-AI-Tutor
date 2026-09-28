package com.aitutor.dto;

import lombok.Data;

@Data
public class PeerReviewDTO {
    private Long revieweeId;
    private Long taskId;
    private String scoresJson;
    private String comments;
}
