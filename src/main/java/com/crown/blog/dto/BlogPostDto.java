package com.crown.blog.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class BlogPostDto {
    private Long postId;
    private Long memberId;
    private String subject;
    private String content;
    private List<String> mediaUrls;
    private String additionalInfo;
    private String platform;       // naver | tistory
    private String status;         // draft | generating | ready | published | scheduled | error
    private String errorMessage;
    private String publishedUrl;
    private LocalDateTime scheduledAt;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
