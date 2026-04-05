package com.crown.omok.service;

import com.crown.omok.dto.GameMoveDto;
import com.crown.omok.dto.GameRoomDto;

import java.util.List;

public interface OmokService {
    GameRoomDto createRoom(Long blackMemberId, Long whiteMemberId);
    void saveMove(GameMoveDto gameMoveDto);
    void finishGame(Long roomId, Long winnerId);   // winnerId null = 무승부
    GameRoomDto findRoom(Long roomId);
    List<GameRoomDto> findHistory(Long memberId);
}
