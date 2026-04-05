package com.crown.omok.serviceimpl;

import com.crown.omok.dto.GameRoomDto;
import com.crown.omok.service.MatchingService;
import com.crown.omok.service.OmokService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Deque;

@Service
@RequiredArgsConstructor
public class MatchingServiceImpl implements MatchingService {

    private final OmokService omokService;

    // 인메모리 대기열 (단일 서버 기준)
    private final Deque<Long> waitingQueue = new ArrayDeque<>();

    @Override
    public synchronized GameRoomDto enqueue(Long memberId) {
        if (waitingQueue.contains(memberId)) {
            return null;
        }

        if (!waitingQueue.isEmpty()) {
            Long opponentId = waitingQueue.poll();
            // 먼저 들어온 사람이 흑돌
            return omokService.createRoom(opponentId, memberId);
        }

        waitingQueue.add(memberId);
        return null;
    }

    @Override
    public synchronized void dequeue(Long memberId) {
        waitingQueue.remove(memberId);
    }
}
