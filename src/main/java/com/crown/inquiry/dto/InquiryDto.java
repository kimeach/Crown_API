package com.crown.inquiry.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class InquiryDto {
    private Long inquiryId;
    private Long memberId;
    private String title;
    private String content;
    private String status;
    private LocalDateTime createdAt;
}
