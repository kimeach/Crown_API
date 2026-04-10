package com.crown.instagram.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class InstagramAccountDto {
    private Long id;
    private Long memberId;
    private String instagramUserId;
    private String username;
    private String accessToken;
    private String tokenType;
    private Long expiresIn;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}