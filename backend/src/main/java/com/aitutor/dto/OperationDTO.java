package com.aitutor.dto;

import lombok.Data;

@Data
public class OperationDTO {
    private Long lessonPackId;
    private Long materialId;
    private String actionType;
    private String actionDetail;
    private Integer duration;
}
