package com.crown.billing.dto;

import lombok.Data;
import java.time.LocalDateTime;

public class BillingDto {

    /** 구독 조회 응답 */
    @Data
    public static class SubscriptionResponse {
        private Integer id;
        private Long memberId;
        private String plan;          // free, pro, team, business
        private String billingCycle;   // monthly, yearly
        private String status;         // active, cancelled, expired, pending
        private LocalDateTime startedAt;
        private LocalDateTime nextBillingAt;
        private LocalDateTime cancelledAt;
        private LocalDateTime expiresAt;
        private String cardCompany;
        private String cardLast4;
    }

    /** 결제 내역 응답 */
    @Data
    public static class PaymentResponse {
        private Integer id;
        private String orderName;
        private Integer amount;
        private String status;         // done, cancelled, failed, refunded
        private String method;
        private String cardLast4;
        private LocalDateTime approvedAt;
        private String receiptUrl;
    }

    /** 주문 생성 요청 */
    @Data
    public static class CheckoutRequest {
        private String plan;           // pro, team, business
        private String billingCycle;   // monthly, yearly
    }

    /** 주문 생성 응답 (프론트 → 토스 SDK로 전달) */
    @Data
    public static class CheckoutResponse {
        private String orderId;
        private String orderName;
        private Integer amount;
        private String customerKey;
        private String clientKey;
        private String successUrl;
        private String failUrl;
    }

    /** 결제 승인 요청 (프론트에서 토스 인증 후) */
    @Data
    public static class ConfirmRequest {
        private String paymentKey;
        private String orderId;
        private Integer amount;
    }

    /** 빌링키 발급 요청 */
    @Data
    public static class BillingKeyRequest {
        private String authKey;
        private String customerKey;
    }

    /** 구독 해지 요청 */
    @Data
    public static class CancelRequest {
        private String reason;
    }

    /** 웹훅 이벤트 */
    @Data
    public static class WebhookEvent {
        private String eventType;
        private Object data;
    }
}
