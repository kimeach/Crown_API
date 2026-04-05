package com.crown.omok.mapper;

import com.crown.omok.dto.GameMoveDto;
import com.crown.omok.dto.GameRoomDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OmokMapper {
    void insertRoom(@Param("blackMemberId") Long blackMemberId,
                    @Param("whiteMemberId") Long whiteMemberId);
    GameRoomDto findLatestRoom(@Param("blackMemberId") Long blackMemberId,
                               @Param("whiteMemberId") Long whiteMemberId);
    GameRoomDto findRoomById(@Param("roomId") Long roomId);
    void finishRoom(@Param("roomId") Long roomId,
                    @Param("winnerId") Long winnerId);
    void insertMove(GameMoveDto gameMoveDto);
    List<GameRoomDto> findHistoryByMemberId(@Param("memberId") Long memberId);
}
