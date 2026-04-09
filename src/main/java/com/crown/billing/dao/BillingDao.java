package com.crown.billing.dao;

import com.crown.billing.dto.BillingDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface BillingDao {

    // ── 구독 ──
    BillingDto.SubscriptionResponse getSubscription(@Param("memberId") Long memberId);

    void insertSubscription(Map<String, Object> params);

    void updateSubscription(Map<String, Object> params);

    void cancelSubscription(@Param("memberId") Long memberId,
                            @Param("reason") String reason,
                            @Param("expiresAt") LocalDateTime expiresAt);

    /** 자동결제 대상 조회 (next_billing_at <= NOW, status = active) */
    List<Map<String, Object>> getDueBillings();

    void updateNextBilling(@Param("memberId") Long memberId,
                           @Param("nextBillingAt") LocalDateTime nextBillingAt);

    // ── 결제 ──
    void insertPayment(Map<String, Object> params);

    void updatePaymentStatus(@Param("orderId") String orderId,
                             @Param("status") String status,
                             @Param("raw") String rawResponse);

    void updatePaymentApproval(Map<String, Object> params);

    List<BillingDto.PaymentResponse> getPaymentHistory(@Param("memberId") Long memberId,
                                                        @Param("limit") int limit);

    Map<String, Object> getPaymentByOrderId(@Param("orderId") String orderId);

    // ── 사용량 ──
    Map<String, Object> getUsage(@Param("memberId") Long memberId,
                                 @Param("yearMonth") String yearMonth);

    void upsertUsage(Map<String, Object> params);

    void incrementUsage(@Param("memberId") Long memberId,
                        @Param("yearMonth") String yearMonth,
                        @Param("column") String column);
}
