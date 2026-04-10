package com.crown.billing.controller;

import com.crown.billing.service.ReferralService;
import com.crown.common.dto.ApiResponse;
import com.crown.member.dto.MemberDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/referral")
@RequiredArgsConstructor
public class ReferralController {

    private final ReferralService referralService;

    /** 내 초대 코드 조회/생성 */
    @GetMapping("/my-code")
    public ResponseEntity<?> getMyCode(@AuthenticationPrincipal MemberDto member) {
        String code = referralService.generateCode(member.getMemberId());
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
            "code", code,
            "ownerName", member.getNickname() != null ? member.getNickname() : "회원"
        )));
    }

    /** 초대 코드 유효성 검증 (인증 불필요) */
    @GetMapping("/validate/{code}")
    public ResponseEntity<?> validateCode(@PathVariable String code) {
        Map<String, Object> result = referralService.validateCode(code);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /** 내 초대 통계 */
    @GetMapping("/stats")
    public ResponseEntity<?> getStats(@AuthenticationPrincipal MemberDto member) {
        Map<String, Object> stats = referralService.getMyStats(member.getMemberId());
        return ResponseEntity.ok(ApiResponse.ok(stats));
    }

    /** 초대 이력 (페이지네이션) */
    @GetMapping("/history")
    public ResponseEntity<?> getHistory(
            @AuthenticationPrincipal MemberDto member,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Map<String, Object> history = referralService.getHistory(member.getMemberId(), page, size);
        return ResponseEntity.ok(ApiResponse.ok(history));
    }
}
