package com.crown.common.controller;

import com.crown.common.dto.ApiResponse;
import com.crown.common.scheduler.FeatureProposalScheduler;
import com.crown.member.service.MemberService;
import com.google.firebase.auth.FirebaseToken;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/scheduler")
@RequiredArgsConstructor
public class SchedulerController {

    private final FeatureProposalScheduler featureProposalScheduler;
    private final MemberService memberService;
    private final RestTemplate restTemplate;

    @Value("${admin.emails:kimeach94@gmail.com}")
    private String adminEmails;

    @Value("${google.sheets.webhook}")
    private String sheetsWebhook;

    @Value("${google.sheets.token}")
    private String sheetsToken;

    @PostMapping("/feature-proposal/run")
    public ApiResponse<String> runFeatureProposal(@AuthenticationPrincipal FirebaseToken token) {
        String email = token.getEmail();
        if (email == null || !adminEmails.contains(email)) {
            throw new IllegalArgumentException("관리자만 실행 가능합니다.");
        }
        featureProposalScheduler.runDailyFeatureProposal();
        return ApiResponse.ok("기능 제안 실행 시작됨");
    }

    @PostMapping("/feature-proposal/status")
    public ApiResponse<String> updateFeatureStatus(
            @RequestHeader("X-Worker-Secret") String secret,
            @RequestParam String featureName,
            @RequestParam String status,
            @RequestParam(required = false) String branch) {

        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("token", sheetsToken);
            payload.put("type", "feature_status_update");
            payload.put("feature_name", featureName);
            payload.put("status", status);
            payload.put("branch", branch != null ? branch : "");
            payload.put("date", LocalDate.now().toString());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            restTemplate.postForEntity(sheetsWebhook, new HttpEntity<>(payload, headers), String.class);

            return ApiResponse.ok("상태 업데이트 완료: " + featureName + " → " + status);
        } catch (Exception e) {
            return ApiResponse.ok("Sheets 업데이트 실패 (무시): " + e.getMessage());
        }
    }
}
