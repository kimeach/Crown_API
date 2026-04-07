package com.crown.common.service;

import com.google.firebase.messaging.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class FcmService {

    private final JdbcTemplate jdbcTemplate;

    public FcmService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 만료/미등록 토큰이면 DB에서 삭제 */
    private boolean isStaleToken(Exception e) {
        if (e instanceof FirebaseMessagingException fme) {
            MessagingErrorCode code = fme.getMessagingErrorCode();
            return code == MessagingErrorCode.UNREGISTERED
                    || code == MessagingErrorCode.INVALID_ARGUMENT;
        }
        String msg = e.getMessage();
        return msg != null && (msg.contains("UNREGISTERED")
                || msg.contains("Requested entity was not found")
                || msg.contains("not found"));
    }

    private void clearStaleToken(String fcmToken) {
        try {
            int updated = jdbcTemplate.update(
                    "UPDATE member SET fcm_token = NULL WHERE fcm_token = ?", fcmToken);
            if (updated > 0) log.info("[FCM] 만료 토큰 삭제: {}...", fcmToken.substring(0, Math.min(10, fcmToken.length())));
        } catch (Exception ex) {
            log.warn("[FCM] 토큰 삭제 실패: {}", ex.getMessage());
        }
    }

    /** 단건 발송 (프로젝트 완료 알림 등) */
    public void send(String fcmToken, String title, String body, String projectId) {
        if (fcmToken == null || fcmToken.isBlank()) return;
        try {
            Message message = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .putData("projectId", projectId != null ? projectId : "")
                    .build();
            String response = FirebaseMessaging.getInstance().send(message);
            log.info("[FCM] 전송 성공: {}", response);
        } catch (Exception e) {
            log.warn("[FCM] 전송 실패 (token={}...): {}",
                    fcmToken.substring(0, Math.min(10, fcmToken.length())), e.getMessage());
            if (isStaleToken(e)) clearStaleToken(fcmToken);
        }
    }

    /**
     * 전체 사용자 일괄 발송 (공지 등)
     * Firebase 제한: 1회 최대 500 토큰 → 배치 처리
     *
     * @return 실제 전송 성공 수
     */
    public int sendToAll(String title, String body, List<String> tokens) {
        if (tokens == null || tokens.isEmpty()) return 0;

        List<String> valid = tokens.stream()
                .filter(t -> t != null && !t.isBlank())
                .toList();
        if (valid.isEmpty()) return 0;

        int totalSent = 0;
        int batchSize = 500;

        for (int i = 0; i < valid.size(); i += batchSize) {
            List<String> batch = valid.subList(i, Math.min(i + batchSize, valid.size()));
            try {
                MulticastMessage multicast = MulticastMessage.builder()
                        .addAllTokens(batch)
                        .setNotification(Notification.builder()
                                .setTitle(title)
                                .setBody(body)
                                .build())
                        .build();
                BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(multicast);
                totalSent += response.getSuccessCount();
                log.info("[FCM] 멀티캐스트 배치 {}/{} — 성공 {}/{} 건",
                        i / batchSize + 1, (valid.size() - 1) / batchSize + 1,
                        response.getSuccessCount(), batch.size());
                // 만료 토큰 정리
                List<SendResponse> results = response.getResponses();
                for (int j = 0; j < results.size(); j++) {
                    SendResponse sr = results.get(j);
                    if (!sr.isSuccessful() && sr.getException() != null && isStaleToken(sr.getException())) {
                        clearStaleToken(batch.get(j));
                    }
                }
            } catch (Exception e) {
                log.warn("[FCM] 멀티캐스트 배치 전송 실패: {}", e.getMessage());
            }
        }

        log.info("[FCM] 전체 발송 완료: 총 {}명 중 {}명 성공", valid.size(), totalSent);
        return totalSent;
    }
}
