package com.crown.billing.controller;

import com.crown.billing.dto.BillingDto;
import com.crown.billing.service.BillingService;
import com.crown.common.dto.ApiResponse;
import com.crown.member.dto.MemberDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/billing")
@RequiredArgsConstructor
public class BillingController {

    private final BillingService billingService;

    /** 현재 구독 조회 */
    @GetMapping("/subscription")
    public ResponseEntity<?> getSubscription(@AuthenticationPrincipal MemberDto member) {
        BillingDto.SubscriptionResponse sub = billingService.getSubscription(member.getMemberId());
        return ResponseEntity.ok(ApiResponse.success(sub));
    }

    /** 주문 생성 (결제 전 — 프론트에서 토스 SDK 호출에 필요한 정보 반환) */
    @PostMapping("/checkout")
    public ResponseEntity<?> createCheckout(
            @AuthenticationPrincipal MemberDto member,
            @RequestBody BillingDto.CheckoutRequest req) {
        BillingDto.CheckoutResponse res = billingService.createCheckout(member.getMemberId(), req);
        return ResponseEntity.ok(ApiResponse.success(res));
    }

    /** 결제 승인 (프론트에서 토스 인증 성공 후 호출) */
    @PostMapping("/confirm")
    public ResponseEntity<?> confirmPayment(
            @AuthenticationPrincipal MemberDto member,
            @RequestBody BillingDto.ConfirmRequest req) {
        Map<String, Object> result = billingService.confirmPayment(member.getMemberId(), req);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /** 빌링키 발급 + 구독 시작 (정기결제용) */
    @PostMapping("/subscribe")
    public ResponseEntity<?> subscribe(
            @AuthenticationPrincipal MemberDto member,
            @RequestBody Map<String, String> body) {
        String authKey = body.get("authKey");
        String plan = body.get("plan");
        String billingCycle = body.get("billingCycle");
        billingService.activateSubscription(member.getMemberId(), authKey, plan, billingCycle);
        return ResponseEntity.ok(ApiResponse.success("구독이 시작되었습니다"));
    }

    /** 구독 해지 */
    @PostMapping("/subscription/cancel")
    public ResponseEntity<?> cancelSubscription(
            @AuthenticationPrincipal MemberDto member,
            @RequestBody(required = false) BillingDto.CancelRequest req) {
        String reason = req != null ? req.getReason() : null;
        billingService.cancelSubscription(member.getMemberId(), reason);
        return ResponseEntity.ok(ApiResponse.success("구독이 해지되었습니다"));
    }

    /** 결제 내역 조회 */
    @GetMapping("/history")
    public ResponseEntity<?> getPaymentHistory(@AuthenticationPrincipal MemberDto member) {
        List<BillingDto.PaymentResponse> history = billingService.getPaymentHistory(member.getMemberId());
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    /** 현재 사용량 조회 */
    @GetMapping("/usage")
    public ResponseEntity<?> getUsage(@AuthenticationPrincipal MemberDto member) {
        Map<String, Object> usage = billingService.getCurrentUsage(member.getMemberId());
        return ResponseEntity.ok(ApiResponse.success(usage));
    }

    /** 토스페이먼츠 웹훅 (인증 불필요 — SecurityConfig에서 permitAll 처리) */
    @PostMapping("/webhook")
    public ResponseEntity<?> handleWebhook(@RequestBody BillingDto.WebhookEvent event) {
        log.info("[Billing Webhook] eventType={}", event.getEventType());
        // TODO: 웹훅 시크릿 검증 + 이벤트 처리
        return ResponseEntity.ok("OK");
    }
}
