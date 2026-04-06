package com.crown.common.controller;

import com.crown.common.dto.ApiResponse;
import com.crown.common.scheduler.FeatureProposalScheduler;
import com.crown.member.service.MemberService;
import com.google.firebase.auth.FirebaseToken;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/scheduler")
@RequiredArgsConstructor
public class SchedulerController {

    private final FeatureProposalScheduler featureProposalScheduler;
    private final MemberService memberService;

    @Value("${admin.emails:kimeach94@gmail.com}")
    private String adminEmails;

    @PostMapping("/feature-proposal/run")
    public ApiResponse<String> runFeatureProposal(@AuthenticationPrincipal FirebaseToken token) {
        String email = token.getEmail();
        if (email == null || !adminEmails.contains(email)) {
            throw new IllegalArgumentException("관리자만 실행 가능합니다.");
        }
        featureProposalScheduler.runDailyFeatureProposal();
        return ApiResponse.ok("기능 제안 실행 시작됨");
    }
}
