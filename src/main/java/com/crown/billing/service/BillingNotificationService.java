package com.crown.billing.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * 결제/구독 이벤트 알림.
 * Phase 1: Slack 웹훅으로 관리자 알림.
 * Phase 2: JavaMailSender 또는 AWS SES로 사용자 이메일 발송 (TODO).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BillingNotificationService {

    private final RestTemplate restTemplate;

    @Value("${slack.billing.webhook:}")
    private String slackWebhook;

    // ═══════ 구독 시작 ═══════

    public void onSubscriptionStarted(Long memberId, String plan, String cycle, int amount) {
        String msg = String.format(
            ":tada: *구독 시작* — memberId=%d\n플랜: %s (%s)\n금액: %,d원",
            memberId, plan.toUpperCase(), cycle, amount
        );
        sendSlack(msg);
        // TODO: 사용자 이메일 — 환영 + 영수증
    }

    // ═══════ 결제 성공 ═══════

    public void onPaymentSuccess(Long memberId, String orderName, int amount) {
        String msg = String.format(
            ":white_check_mark: *결제 성공* — memberId=%d\n상품: %s\n금액: %,d원",
            memberId, orderName, amount
        );
        sendSlack(msg);
        // TODO: 사용자 이메일 — 영수증
    }

    // ═══════ 결제 실패 ═══════

    public void onPaymentFailed(Long memberId, String orderName, String reason) {
        String msg = String.format(
            ":x: *결제 실패* — memberId=%d\n상품: %s\n사유: %s",
            memberId, orderName, reason
        );
        sendSlack(msg);
        // TODO: 사용자 이메일 — 결제 수단 변경 안내
    }

    // ═══════ 결제일 사전 안내 (7일 전) ═══════

    public void onUpcomingBilling(Long memberId, String plan, int amount, String billingDate) {
        // TODO: 사용자 이메일 — "OO님, 7일 후 자동결제 예정입니다"
        log.info("[BillingNotify] 사전안내: memberId={}, plan={}, billingDate={}", memberId, plan, billingDate);
    }

    // ═══════ 구독 해지 ═══════

    public void onSubscriptionCancelled(Long memberId, String plan, String expiresAt) {
        String msg = String.format(
            ":wave: *구독 해지* — memberId=%d\n플랜: %s\n이용 가능: %s까지",
            memberId, plan.toUpperCase(), expiresAt
        );
        sendSlack(msg);
        // TODO: 사용자 이메일 — 해지 확인 + 이용 가능 기간 안내
    }

    // ═══════ Slack 발송 ═══════

    private void sendSlack(String text) {
        if (slackWebhook == null || slackWebhook.isBlank()) {
            log.debug("[BillingNotify] Slack 웹훅 미설정, 로그만 출력: {}", text);
            return;
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(Map.of("text", text), headers);
            restTemplate.postForEntity(slackWebhook, entity, String.class);
        } catch (Exception e) {
            log.warn("[BillingNotify] Slack 발송 실패: {}", e.getMessage());
        }
    }
}
