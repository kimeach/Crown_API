package com.crown.omok.dao;

import com.crown.omok.dto.GameMoveDto;
import com.crown.omok.dto.GameRoomDto;
import com.crown.omok.mapper.OmokMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class OmokDao {

    private final OmokMapper omokMapper;

    public void insertRoom(Long blackMemberId, Long whiteMemberId) {
        omokMapper.insertRoom(blackMemberId, whiteMemberId);
    }

    public GameRoomDto findLatestRoom(Long blackMemberId, Long whiteMemberId) {
        return omokMapper.findLatestRoom(blackMemberId, whiteMemberId);
    }

    public GameRoomDto findRoomById(Long roomId) {
        return omokMapper.findRoomById(roomId);
    }

    public void finishRoom(Long roomId, Long winnerId) {
        omokMapper.finishRoom(roomId, winnerId);
    }

    public void insertMove(GameMoveDto gameMoveDto) {
        omokMapper.insertMove(gameMoveDto);
    }

    public List<GameRoomDto> findHistoryByMemberId(Long memberId) {
        return omokMapper.findHistoryByMemberId(memberId);
    }
}
