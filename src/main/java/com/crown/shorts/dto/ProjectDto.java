package com.crown.shorts.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

@Data
public class ProjectDto {
    private Long projectId;
    private Long memberId;
    private String category;
    private String title;
    private Map<String, String> script;  // {slide_1: "...", slide_2: "..."}
    private String templateId;
    private java.util.Map<String, Object> options;
    private String htmlUrl;
    private String status;               // draft | generating | done | error
    private String videoUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
