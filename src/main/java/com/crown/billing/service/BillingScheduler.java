package com.crown.billing.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 자동결제 스케줄러.
 * 매일 새벽 2시(KST) — next_billing_at이 지난 활성 구독에 대해 빌링키 결제 실행.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BillingScheduler {

    private final BillingService billingService;

    @Scheduled(cron = "0 0 2 * * *", zone = "Asia/Seoul")
    public void processAutoBilling() {
        log.info("[BillingScheduler] 자동결제 처리 시작");
        try {
            billingService.processDueBillings();
        } catch (Exception e) {
            log.error("[BillingScheduler] 자동결제 처리 실패: {}", e.getMessage(), e);
        }
        log.info("[BillingScheduler] 자동결제 처리 완료");
    }
}
