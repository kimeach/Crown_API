package com.crown.shorts.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProgressMessage {
    private final Long projectId;
    private final Long jobId;
    private final int percent;       // 0~100
    private final String step;       // TTS / CAPTURE / ENCODE / UPLOAD / DONE / ERROR
    private final String message;
}
