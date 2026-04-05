package com.crown.omok.serviceimpl;

import com.crown.omok.dao.OmokDao;
import com.crown.omok.dto.GameMoveDto;
import com.crown.omok.dto.GameRoomDto;
import com.crown.omok.service.EloService;
import com.crown.omok.service.OmokService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OmokServiceImpl implements OmokService {

    private final OmokDao    omokDao;
    private final EloService eloService;

    @Override
    public GameRoomDto createRoom(Long blackMemberId, Long whiteMemberId) {
        omokDao.insertRoom(blackMemberId, whiteMemberId);
        return omokDao.findLatestRoom(blackMemberId, whiteMemberId);
    }

    @Override
    public void saveMove(GameMoveDto gameMoveDto) {
        omokDao.insertMove(gameMoveDto);
    }

    @Override
    public void finishGame(Long roomId, Long winnerId) {
        GameRoomDto room = omokDao.findRoomById(roomId);
        omokDao.finishRoom(roomId, winnerId);

        if (winnerId == null) {
            eloService.calculateDraw(room.getBlackMemberId(), room.getWhiteMemberId());
        } else {
            Long loserId = winnerId.equals(room.getBlackMemberId())
                    ? room.getWhiteMemberId()
                    : room.getBlackMemberId();
            eloService.calculate(winnerId, loserId);
        }
    }

    @Override
    public GameRoomDto findRoom(Long roomId) {
        return omokDao.findRoomById(roomId);
    }

    @Override
    public List<GameRoomDto> findHistory(Long memberId) {
        return omokDao.findHistoryByMemberId(memberId);
    }
}
