package com.crown.shorts.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class JobDto {
    private Long jobId;
    private Long projectId;
    private String status;           // pending | running | done | error
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}
