package com.crown.omok.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GameMoveDto {
    private Long   roomId;
    private Long   memberId;
    private int    moveX;
    private int    moveY;
    private int    moveOrder;
}
