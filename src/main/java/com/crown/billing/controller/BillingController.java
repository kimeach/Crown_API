package com.crown.billing.controller;

import com.crown.billing.dto.BillingDto;
import com.crown.billing.service.BillingService;
import com.crown.billing.service.TokenService;
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
    private final TokenService tokenService;

    /** 현재 구독 조회 */
    @GetMapping("/subscription")
    public ResponseEntity<?> getSubscription(@AuthenticationPrincipal MemberDto member) {
        BillingDto.SubscriptionResponse sub = billingService.getSubscription(member.getMemberId());
        return ResponseEntity.ok(ApiResponse.ok(sub));
    }

    /** 주문 생성 (결제 전 — 프론트에서 토스 SDK 호출에 필요한 정보 반환) */
    @PostMapping("/checkout")
    public ResponseEntity<?> createCheckout(
            @AuthenticationPrincipal MemberDto member,
            @RequestBody BillingDto.CheckoutRequest req) {
        BillingDto.CheckoutResponse res = billingService.createCheckout(member.getMemberId(), req);
        return ResponseEntity.ok(ApiResponse.ok(res));
    }

    /** 결제 승인 (프론트에서 토스 인증 성공 후 호출) */
    @PostMapping("/confirm")
    public ResponseEntity<?> confirmPayment(
            @AuthenticationPrincipal MemberDto member,
            @RequestBody BillingDto.ConfirmRequest req) {
        Map<String, Object> result = billingService.confirmPayment(member.getMemberId(), req);
        return ResponseEntity.ok(ApiResponse.ok(result));
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
        return ResponseEntity.ok(ApiResponse.ok("구독이 시작되었습니다"));
    }

    /** 구독 해지 */
    @PostMapping("/subscription/cancel")
    public ResponseEntity<?> cancelSubscription(
            @AuthenticationPrincipal MemberDto member,
            @RequestBody(required = false) BillingDto.CancelRequest req) {
        String reason = req != null ? req.getReason() : null;
        billingService.cancelSubscription(member.getMemberId(), reason);
        return ResponseEntity.ok(ApiResponse.ok("구독이 해지되었습니다"));
    }

    /** 결제 내역 조회 */
    @GetMapping("/history")
    public ResponseEntity<?> getPaymentHistory(@AuthenticationPrincipal MemberDto member) {
        List<BillingDto.PaymentResponse> history = billingService.getPaymentHistory(member.getMemberId());
        return ResponseEntity.ok(ApiResponse.ok(history));
    }

    /** 현재 사용량 조회 */
    @GetMapping("/usage")
    public ResponseEntity<?> getUsage(@AuthenticationPrincipal MemberDto member) {
        Map<String, Object> usage = billingService.getCurrentUsage(member.getMemberId());
        return ResponseEntity.ok(ApiResponse.ok(usage));
    }

    /** 토스페이먼츠 웹훅 (인증 불필요 — SecurityConfig에서 permitAll 처리) */
    @PostMapping("/webhook")
    public ResponseEntity<?> handleWebhook(@RequestBody BillingDto.WebhookEvent event) {
        log.info("[Billing Webhook] eventType={}", event.getEventType());
        // TODO: 웹훅 시크릿 검증 + 이벤트 처리
        return ResponseEntity.ok("OK");
    }

    // ══════════════════════ 토큰 API ══════════════════════

    /** 토큰 잔액 조회 */
    @GetMapping("/tokens")
    public ResponseEntity<?> getTokenBalance(@AuthenticationPrincipal MemberDto member) {
        Map<String, Object> balance = tokenService.getBalance(member.getMemberId());
        return ResponseEntity.ok(ApiResponse.ok(balance));
    }

    /** 토큰 사용 이력 조회 (페이지네이션) */
    @GetMapping("/tokens/history")
    public ResponseEntity<?> getTokenHistory(
            @AuthenticationPrincipal MemberDto member,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<Map<String, Object>> history = tokenService.getHistory(member.getMemberId(), page, size);
        int total = tokenService.getHistoryCount(member.getMemberId());
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "list", history,
                "total", total,
                "page", page,
                "size", size
        )));
    }

    /** 토큰 잔액 충분 여부 확인 */
    @GetMapping("/tokens/check")
    public ResponseEntity<?> checkTokens(
            @AuthenticationPrincipal MemberDto member,
            @RequestParam int amount) {
        boolean enough = tokenService.hasEnoughTokens(member.getMemberId(), amount);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("enough", enough)));
    }

    /** 플랜 설정 목록 조회 (인증 불필요) */
    @GetMapping("/plans")
    public ResponseEntity<?> getPlans() {
        return ResponseEntity.ok(ApiResponse.ok(tokenService.getAllPlanConfigs()));
    }
}
