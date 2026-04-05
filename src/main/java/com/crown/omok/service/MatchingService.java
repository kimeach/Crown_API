package com.crown.omok.service;

import com.crown.omok.dto.GameRoomDto;

public interface MatchingService {
    /** 대기열에 입장. 매칭 상대가 있으면 GameRoomDto 반환, 없으면 null */
    GameRoomDto enqueue(Long memberId);

    /** 대기열에서 이탈 */
    void dequeue(Long memberId);
}
