package com.crown.blog.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class BlogToneDto {
    private Long toneId;
    private Long memberId;
    private String style;
    private List<String> characteristics;
    private String sampleText;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
