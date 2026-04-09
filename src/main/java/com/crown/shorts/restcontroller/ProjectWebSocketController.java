package com.crown.shorts.restcontroller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 프로젝트 실시간 협업 WebSocket 컨트롤러
 * - presence: 현재 접속자 추적
 * - changes: 편집 알림 브로드캐스트
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class ProjectWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;

    // projectId → Set<접속자 정보 JSON>
    private final ConcurrentHashMap<Long, Set<String>> presenceMap = new ConcurrentHashMap<>();

    /**
     * 접속자 입장: /app/project/{projectId}/join
     * payload: { "memberId": 123, "nickname": "홍길동", "profileImg": "url" }
     */
    @MessageMapping("/project/{projectId}/join")
    public void joinProject(@DestinationVariable Long projectId, @Payload Map<String, Object> payload) {
        String userKey = buildUserKey(payload);
        presenceMap.computeIfAbsent(projectId, k -> ConcurrentHashMap.newKeySet()).add(userKey);

        // 전체 접속자 목록 브로드캐스트
        broadcastPresence(projectId);
        log.info("[Presence] JOIN project={} user={}", projectId, payload.get("memberId"));
    }

    /**
     * 접속자 퇴장: /app/project/{projectId}/leave
     */
    @MessageMapping("/project/{projectId}/leave")
    public void leaveProject(@DestinationVariable Long projectId, @Payload Map<String, Object> payload) {
        String userKey = buildUserKey(payload);
        Set<String> users = presenceMap.get(projectId);
        if (users != null) {
            users.remove(userKey);
            if (users.isEmpty()) presenceMap.remove(projectId);
        }

        broadcastPresence(projectId);
        log.info("[Presence] LEAVE project={} user={}", projectId, payload.get("memberId"));
    }

    /**
     * 편집 알림: /app/project/{projectId}/change
     * payload: { "memberId": 123, "nickname": "홍길동", "action": "script_save" }
     */
    @MessageMapping("/project/{projectId}/change")
    public void notifyChange(@DestinationVariable Long projectId, @Payload Map<String, Object> payload) {
        messagingTemplate.convertAndSend(
                "/topic/project/" + projectId + "/changes",
                payload
        );
    }

    private void broadcastPresence(Long projectId) {
        Set<String> users = presenceMap.getOrDefault(projectId, Set.of());
        messagingTemplate.convertAndSend(
                "/topic/project/" + projectId + "/presence",
                Map.of("users", users, "count", users.size())
        );
    }

    private String buildUserKey(Map<String, Object> payload) {
        Object memberId = payload.getOrDefault("memberId", "0");
        Object nickname = payload.getOrDefault("nickname", "");
        Object profileImg = payload.getOrDefault("profileImg", "");
        return memberId + "|" + nickname + "|" + profileImg;
    }
}
