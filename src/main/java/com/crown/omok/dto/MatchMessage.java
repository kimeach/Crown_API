package com.crown.omok.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * WebSocket 메시지 공통 래퍼
 * type: MATCHED | MOVE | WIN | DRAW | SURRENDER | ERROR
 */
@Getter
@Builder
public class MatchMessage {
    private String type;
    private Object data;
}
