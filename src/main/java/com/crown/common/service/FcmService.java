package com.crown.common.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class FcmService {

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
            log.warn("[FCM] 전송 실패 (token={}...): {}", fcmToken.substring(0, Math.min(10, fcmToken.length())), e.getMessage());
        }
    }
}
