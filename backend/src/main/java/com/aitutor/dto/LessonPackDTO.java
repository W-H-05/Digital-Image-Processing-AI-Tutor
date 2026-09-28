package com.aitutor.dto;

import lombok.Data;

@Data
public class LessonPackDTO {
    private Long courseId;
    private String lessonNo;
    private String hours;
    private String title;
    private String chapter;
    private String objectives;
    private String contentSummary;
    private String keyPoints;
    private String difficultPoints;
    private String ideologicalNotes;
    private String status;
}
