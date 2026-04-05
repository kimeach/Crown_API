package com.crown.shorts.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

@Data
public class ScriptHistoryDto {
    private Long historyId;
    private Long projectId;
    private Long memberId;
    private Map<String, String> script;
    private String note;
    private LocalDateTime createdAt;
}
