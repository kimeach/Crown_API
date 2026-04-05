package com.crown.omok.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class GameRoomDto {
    private Long          roomId;
    private Long          blackMemberId;
    private Long          whiteMemberId;
    private String        status;
    private Long          winnerId;
    private Integer       blackScoreChange;
    private Integer       whiteScoreChange;
    private LocalDateTime createdAt;
    private LocalDateTime finishedAt;
}
