package com.aitutor.dto;

import lombok.Data;

@Data
public class ChallengeDTO {
    private Long lessonPackId;
    private Long coursewareId;
    private String paramsJson;
    private String screenshotPath;
    private String conclusion;
}
