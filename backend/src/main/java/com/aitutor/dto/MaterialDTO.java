package com.aitutor.dto;

import lombok.Data;

@Data
public class MaterialDTO {
    private String materialType;
    private String title;
    private String description;
    private Integer sortIndex;
    private String groupName;
    private Integer isOpenToStudent;
    private Integer allowDownload;
}
