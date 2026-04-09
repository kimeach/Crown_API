package com.crown.billing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * 토스페이먼츠 API 클라이언트.
 * @see <a href="https://docs.tosspayments.com/reference">토스페이먼츠 API 문서</a>
 */
@Slf4j
@Component
public class TossPaymentsClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${toss.payments.secret-key:}")
    private String secretKey;

    @Value("${toss.payments.api-url:https://api.tosspayments.com/v1}")
    private String apiUrl;

    public TossPaymentsClient(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 결제 승인 (일반 결제)
     * POST /v1/payments/confirm
     */
    public Map<String, Object> confirmPayment(String paymentKey, String orderId, int amount) {
        String url = apiUrl + "/payments/confirm";
        Map<String, Object> body = Map.of(
            "paymentKey", paymentKey,
            "orderId", orderId,
            "amount", amount
        );
        return post(url, body);
    }

    /**
     * 빌링키 발급
     * POST /v1/billing/authorizations/issue
     */
    public Map<String, Object> issueBillingKey(String authKey, String customerKey) {
        String url = apiUrl + "/billing/authorizations/issue";
        Map<String, Object> body = Map.of(
            "authKey", authKey,
            "customerKey", customerKey
        );
        return post(url, body);
    }

    /**
     * 빌링키로 자동결제
     * POST /v1/billing/{billingKey}
     */
    public Map<String, Object> executeBilling(String billingKey, String customerKey,
                                               int amount, String orderId, String orderName) {
        String url = apiUrl + "/billing/" + billingKey;
        Map<String, Object> body = Map.of(
            "customerKey", customerKey,
            "amount", amount,
            "orderId", orderId,
            "orderName", orderName
        );
        return post(url, body);
    }

    /**
     * 결제 취소
     * POST /v1/payments/{paymentKey}/cancel
     */
    public Map<String, Object> cancelPayment(String paymentKey, String cancelReason) {
        String url = apiUrl + "/payments/" + paymentKey + "/cancel";
        Map<String, Object> body = Map.of("cancelReason", cancelReason);
        return post(url, body);
    }

    // ── 내부 ──

    @SuppressWarnings("unchecked")
    private Map<String, Object> post(String url, Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Basic " + encodeSecret());

        try {
            String json = objectMapper.writeValueAsString(body);
            HttpEntity<String> entity = new HttpEntity<>(json, headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("[TossPayments] API 호출 실패: {} — {}", url, e.getMessage());
            throw new RuntimeException("토스페이먼츠 API 호출 실패: " + e.getMessage(), e);
        }
    }

    private String encodeSecret() {
        return Base64.getEncoder()
            .encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));
    }
}
