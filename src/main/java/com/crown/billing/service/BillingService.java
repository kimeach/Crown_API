package com.crown.billing.service;

import com.crown.billing.constants.PlanLimits;
import com.crown.billing.dao.BillingDao;
import com.crown.billing.dto.BillingDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillingService {

    private final BillingDao billingDao;
    private final TossPaymentsClient tossClient;
    private final TokenService tokenService;

    @Value("${toss.payments.client-key:}")
    private String clientKey;

    @Value("${app.base-url:http://localhost:3000}")
    private String baseUrl;

    // ═══════ 구독 조회 ═══════

    public BillingDto.SubscriptionResponse getSubscription(Long memberId) {
        BillingDto.SubscriptionResponse sub = billingDao.getSubscription(memberId);
        if (sub == null) {
            sub = new BillingDto.SubscriptionResponse();
            sub.setMemberId(memberId);
            sub.setPlan("free");
            sub.setStatus("none");
        }
        return sub;
    }

    // ═══════ 주문 생성 (결제 전) ═══════

    public BillingDto.CheckoutResponse createCheckout(Long memberId, BillingDto.CheckoutRequest req) {
        String plan = req.getPlan();
        String cycle = req.getBillingCycle();
        int amount = PlanLimits.getPrice(plan, cycle);

        if (amount <= 0) {
            throw new IllegalArgumentException("무료 플랜은 결제가 필요 없습니다");
        }

        String orderId = "VLN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        String orderName = PlanLimits.getOrderName(plan, cycle);
        String customerKey = "cust_" + memberId;

        // pending 상태로 결제 레코드 생성
        Map<String, Object> params = new HashMap<>();
        params.put("memberId", memberId);
        params.put("paymentKey", "");
        params.put("orderId", orderId);
        params.put("orderName", orderName);
        params.put("amount", amount);
        params.put("status", "pending");
        params.put("requestedAt", LocalDateTime.now());
        billingDao.insertPayment(params);

        BillingDto.CheckoutResponse res = new BillingDto.CheckoutResponse();
        res.setOrderId(orderId);
        res.setOrderName(orderName);
        res.setAmount(amount);
        res.setCustomerKey(customerKey);
        res.setClientKey(clientKey);
        res.setSuccessUrl(baseUrl + "/billing/success");
        res.setFailUrl(baseUrl + "/billing/fail");
        return res;
    }

    // ═══════ 결제 승인 ═══════

    @Transactional
    public Map<String, Object> confirmPayment(Long memberId, BillingDto.ConfirmRequest req) {
        // 주문 검증
        Map<String, Object> payment = billingDao.getPaymentByOrderId(req.getOrderId());
        if (payment == null) {
            throw new IllegalArgumentException("주문을 찾을 수 없습니다: " + req.getOrderId());
        }

        int dbAmount = (Integer) payment.get("amount");
        if (dbAmount != req.getAmount()) {
            throw new IllegalArgumentException("결제 금액 불일치");
        }

        // 토스페이먼츠 승인 API 호출
        Map<String, Object> tossResult = tossClient.confirmPayment(
            req.getPaymentKey(), req.getOrderId(), req.getAmount()
        );

        // 결제 성공 → DB 업데이트
        Map<String, Object> updateParams = new HashMap<>();
        updateParams.put("orderId", req.getOrderId());
        updateParams.put("paymentKey", req.getPaymentKey());
        updateParams.put("method", tossResult.get("method"));
        updateParams.put("approvedAt", LocalDateTime.now());
        updateParams.put("raw", tossResult.toString());

        // 카드 정보 추출
        @SuppressWarnings("unchecked")
        Map<String, Object> card = (Map<String, Object>) tossResult.get("card");
        if (card != null) {
            updateParams.put("cardCompany", card.get("company"));
            updateParams.put("cardLast4", card.get("number") != null
                ? card.get("number").toString().substring(card.get("number").toString().length() - 4) : null);
        }
        updateParams.put("receiptUrl", tossResult.get("receipt") != null
            ? ((Map<?, ?>) tossResult.get("receipt")).get("url") : null);

        billingDao.updatePaymentApproval(updateParams);

        return tossResult;
    }

    // ═══════ 빌링키 발급 + 구독 활성화 ═══════

    @Transactional
    public void activateSubscription(Long memberId, String authKey,
                                      String plan, String billingCycle) {
        String customerKey = "cust_" + memberId;

        // 빌링키 발급
        Map<String, Object> billingResult = tossClient.issueBillingKey(authKey, customerKey);
        String billingKey = (String) billingResult.get("billingKey");

        // 다음 결제일 계산
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextBilling = "yearly".equals(billingCycle)
            ? now.plusYears(1) : now.plusMonths(1);

        // 구독 레코드 생성/업데이트
        Map<String, Object> params = new HashMap<>();
        params.put("memberId", memberId);
        params.put("plan", plan);
        params.put("billingCycle", billingCycle);
        params.put("status", "active");
        params.put("billingKey", billingKey);
        params.put("customerKey", customerKey);
        params.put("startedAt", now);
        params.put("nextBillingAt", nextBilling);
        billingDao.insertSubscription(params);

        // 토큰 충전
        tokenService.grantMonthlyTokens(memberId, plan);

        log.info("[Billing] 구독 활성화: memberId={}, plan={}, cycle={}", memberId, plan, billingCycle);
    }

    // ═══════ 구독 해지 ═══════

    @Transactional
    public void cancelSubscription(Long memberId, String reason) {
        BillingDto.SubscriptionResponse sub = billingDao.getSubscription(memberId);
        if (sub == null || !"active".equals(sub.getStatus())) {
            throw new IllegalStateException("활성 구독이 없습니다");
        }

        // 현재 결제 기간 끝까지 이용 가능
        LocalDateTime expiresAt = sub.getNextBillingAt() != null
            ? sub.getNextBillingAt() : LocalDateTime.now();

        billingDao.cancelSubscription(memberId, reason, expiresAt);
        log.info("[Billing] 구독 해지: memberId={}, reason={}, expiresAt={}", memberId, reason, expiresAt);
    }

    // ═══════ 결제 내역 ═══════

    public List<BillingDto.PaymentResponse> getPaymentHistory(Long memberId) {
        return billingDao.getPaymentHistory(memberId, 24);
    }

    // ═══════ 사용량 ═══════

    public Map<String, Object> getCurrentUsage(Long memberId) {
        String yearMonth = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        Map<String, Object> usage = billingDao.getUsage(memberId, yearMonth);
        if (usage == null) {
            usage = Map.of(
                "shorts_count", 0, "longform_count", 0,
                "voice_clone_count", 0, "ai_rewrite_count", 0
            );
        }
        return usage;
    }

    public void incrementUsage(Long memberId, String usageType) {
        String yearMonth = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        billingDao.incrementUsage(memberId, yearMonth, usageType);
    }

    public String getActivePlan(Long memberId) {
        BillingDto.SubscriptionResponse sub = billingDao.getSubscription(memberId);
        if (sub == null || !"active".equals(sub.getStatus())) {
            return "free";
        }
        return sub.getPlan();
    }

    // ═══════ 자동결제 처리 (스케줄러에서 호출) ═══════

    @Transactional
    public void processDueBillings() {
        List<Map<String, Object>> dueBillings = billingDao.getDueBillings();

        for (Map<String, Object> billing : dueBillings) {
            Long memberId = ((Number) billing.get("member_id")).longValue();
            String plan = (String) billing.get("plan");
            String cycle = (String) billing.get("billing_cycle");
            String billingKey = (String) billing.get("billing_key");
            String customerKey = (String) billing.get("customer_key");

            try {
                int amount = PlanLimits.getPrice(plan, cycle);
                String orderId = "VLN-AUTO-" + UUID.randomUUID().toString()
                    .replace("-", "").substring(0, 12).toUpperCase();
                String orderName = PlanLimits.getOrderName(plan, cycle);

                // 토스 빌링키 결제
                Map<String, Object> result = tossClient.executeBilling(
                    billingKey, customerKey, amount, orderId, orderName
                );

                // 결제 기록 저장
                Map<String, Object> payParams = new HashMap<>();
                payParams.put("memberId", memberId);
                payParams.put("paymentKey", result.get("paymentKey"));
                payParams.put("orderId", orderId);
                payParams.put("orderName", orderName);
                payParams.put("amount", amount);
                payParams.put("status", "done");
                payParams.put("method", result.get("method"));
                payParams.put("requestedAt", LocalDateTime.now());
                payParams.put("approvedAt", LocalDateTime.now());
                payParams.put("raw", result.toString());
                billingDao.insertPayment(payParams);

                // 다음 결제일 갱신
                LocalDateTime nextBilling = "yearly".equals(cycle)
                    ? LocalDateTime.now().plusYears(1) : LocalDateTime.now().plusMonths(1);
                billingDao.updateNextBilling(memberId, nextBilling);

                log.info("[Billing] 자동결제 성공: memberId={}, amount={}", memberId, amount);
            } catch (Exception e) {
                log.error("[Billing] 자동결제 실패: memberId={} — {}", memberId, e.getMessage());
                // TODO: 재시도 로직, 실패 이메일 발송
            }
        }
    }
}
