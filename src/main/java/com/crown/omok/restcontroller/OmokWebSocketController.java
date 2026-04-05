package com.crown.omok.restcontroller;

import com.crown.member.dto.MemberDto;
import com.crown.member.service.MemberService;
import com.crown.omok.dto.ChallengeDto;
import com.crown.omok.dto.GameMoveDto;
import com.crown.omok.dto.GameRoomDto;
import com.crown.omok.dto.MatchMessage;
import com.crown.omok.service.MatchingService;
import com.crown.omok.service.OmokService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
@RequiredArgsConstructor
public class OmokWebSocketController {

    private final MatchingService       matchingService;
    private final OmokService           omokService;
    private final MemberService         memberService;
    private final SimpMessagingTemplate messagingTemplate;

    /** 매칭 요청: /app/omok/match */
    @MessageMapping("/omok/match")
    public void requestMatch(Long memberId) {
        GameRoomDto room = matchingService.enqueue(memberId);
        if (room != null) {
            MatchMessage matched = MatchMessage.builder()
                    .type("MATCHED")
                    .data(room)
                    .build();
            // 각 플레이어의 유저 토픽으로 전송 (클라이언트가 방 번호를 모르는 상태이므로)
            messagingTemplate.convertAndSend("/topic/omok/user/" + room.getBlackMemberId(), matched);
            messagingTemplate.convertAndSend("/topic/omok/user/" + room.getWhiteMemberId(), matched);
        }
    }

    /** 매칭 취소: /app/omok/match/cancel */
    @MessageMapping("/omok/match/cancel")
    public void cancelMatch(Long memberId) {
        matchingService.dequeue(memberId);
    }

    /** 착수: /app/omok/move */
    @MessageMapping("/omok/move")
    public void move(GameMoveDto gameMoveDto) {
        omokService.saveMove(gameMoveDto);
        MatchMessage moveMessage = MatchMessage.builder()
                .type("MOVE")
                .data(gameMoveDto)
                .build();
        messagingTemplate.convertAndSend("/topic/omok/room/" + gameMoveDto.getRoomId(), moveMessage);
    }

    /** 승리 선언: /app/omok/win */
    @MessageMapping("/omok/win")
    public void win(GameMoveDto gameMoveDto) {
        omokService.finishGame(gameMoveDto.getRoomId(), gameMoveDto.getMemberId());
        MatchMessage winMessage = MatchMessage.builder()
                .type("WIN")
                .data(gameMoveDto.getMemberId())
                .build();
        messagingTemplate.convertAndSend("/topic/omok/room/" + gameMoveDto.getRoomId(), winMessage);
    }

    /** 기권: /app/omok/surrender */
    @MessageMapping("/omok/surrender")
    public void surrender(GameMoveDto gameMoveDto) {
        GameRoomDto room = omokService.findRoom(gameMoveDto.getRoomId());
        Long winnerId = gameMoveDto.getMemberId().equals(room.getBlackMemberId())
                ? room.getWhiteMemberId()
                : room.getBlackMemberId();
        omokService.finishGame(gameMoveDto.getRoomId(), winnerId);
        MatchMessage surrenderMessage = MatchMessage.builder()
                .type("SURRENDER")
                .data(gameMoveDto.getMemberId())
                .build();
        messagingTemplate.convertAndSend("/topic/omok/room/" + gameMoveDto.getRoomId(), surrenderMessage);
    }

    /** 무르기 요청: /app/omok/undo-request */
    @MessageMapping("/omok/undo-request")
    public void undoRequest(GameMoveDto gameMoveDto) {
        MatchMessage msg = MatchMessage.builder()
                .type("UNDO_REQUEST")
                .data(gameMoveDto.getMemberId())
                .build();
        messagingTemplate.convertAndSend("/topic/omok/room/" + gameMoveDto.getRoomId(), msg);
    }

    /** 무르기 응답: /app/omok/undo-response (moveX=1:수락, 0:거절) */
    @MessageMapping("/omok/undo-response")
    public void undoResponse(GameMoveDto gameMoveDto) {
        boolean accepted = gameMoveDto.getMoveX() == 1;
        MatchMessage msg = MatchMessage.builder()
                .type(accepted ? "UNDO_ACCEPT" : "UNDO_REJECT")
                .data(gameMoveDto.getMemberId())
                .build();
        messagingTemplate.convertAndSend("/topic/omok/room/" + gameMoveDto.getRoomId(), msg);
    }

    /** 친구 대전 신청: /app/omok/challenge */
    @MessageMapping("/omok/challenge")
    public void challenge(ChallengeDto dto) {
        MemberDto challenger = memberService.findById(dto.getChallengerId());
        MatchMessage msg = MatchMessage.builder()
                .type("FRIEND_CHALLENGE")
                .data(Map.of("challengerId", dto.getChallengerId(),
                             "nickname",     challenger.getNickname()))
                .build();
        messagingTemplate.convertAndSend("/topic/omok/user/" + dto.getTargetId(), msg);
    }

    /** 친구 대전 수락: /app/omok/challenge/accept */
    @MessageMapping("/omok/challenge/accept")
    public void challengeAccept(ChallengeDto dto) {
        GameRoomDto room = omokService.createRoom(dto.getChallengerId(), dto.getTargetId());
        MatchMessage matched = MatchMessage.builder()
                .type("MATCHED")
                .data(room)
                .build();
        messagingTemplate.convertAndSend("/topic/omok/user/" + dto.getChallengerId(), matched);
        messagingTemplate.convertAndSend("/topic/omok/user/" + dto.getTargetId(), matched);
    }

    /** 친구 대전 거절: /app/omok/challenge/reject */
    @MessageMapping("/omok/challenge/reject")
    public void challengeReject(ChallengeDto dto) {
        MatchMessage msg = MatchMessage.builder()
                .type("FRIEND_CHALLENGE_REJECT")
                .data(dto.getTargetId())
                .build();
        messagingTemplate.convertAndSend("/topic/omok/user/" + dto.getChallengerId(), msg);
    }

    /** 재도전 요청: /app/omok/rematch-request */
    @MessageMapping("/omok/rematch-request")
    public void rematchRequest(GameMoveDto dto) {
        MatchMessage msg = MatchMessage.builder()
                .type("REMATCH_REQUEST")
                .data(dto.getMemberId())
                .build();
        messagingTemplate.convertAndSend("/topic/omok/room/" + dto.getRoomId(), msg);
    }

    /** 재도전 응답: /app/omok/rematch-response (moveX=1:수락, 0:거절) */
    @MessageMapping("/omok/rematch-response")
    public void rematchResponse(GameMoveDto dto) {
        boolean accepted = dto.getMoveX() == 1;
        if (accepted) {
            GameRoomDto oldRoom = omokService.findRoom(dto.getRoomId());
            // 재도전: 흑백 교체
            GameRoomDto newRoom = omokService.createRoom(oldRoom.getWhiteMemberId(), oldRoom.getBlackMemberId());
            MatchMessage msg = MatchMessage.builder()
                    .type("REMATCH_MATCHED")
                    .data(newRoom)
                    .build();
            messagingTemplate.convertAndSend("/topic/omok/room/" + dto.getRoomId(), msg);
        } else {
            MatchMessage msg = MatchMessage.builder()
                    .type("REMATCH_REJECTED")
                    .data(dto.getMemberId())
                    .build();
            messagingTemplate.convertAndSend("/topic/omok/room/" + dto.getRoomId(), msg);
        }
    }
}
