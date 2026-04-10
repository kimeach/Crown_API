package com.crown.instagram.restcontroller;

import com.crown.common.dto.ApiResponse;
import com.crown.instagram.dto.InstagramAccountDto;
import com.crown.instagram.service.InstagramService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/instagram")
@RequiredArgsConstructor
public class InstagramRestController {

    private final InstagramService instagramService;

    /** 인스타그램 연동을 위한 인증 URL 반환 */
    @GetMapping("/auth-url")
    public ApiResponse<String> getAuthUrl() {
        return ApiResponse.ok(instagramService.getAuthorizationUrl());
    }

    /** OAuth 콜백 처리 (프론트엔드에서 전달받은 code 사용) */
    @PostMapping("/callback")
    public ApiResponse<InstagramAccountDto> callback(
            @RequestBody Map<String, String> body,
            @RequestAttribute("memberId") Long memberId) {
        String code = body.get("code");
        return ApiResponse.ok(instagramService.handleOAuthCallback(code, memberId));
    }

    /** 현재 연결된 인스타그램 계정 정보 조회 */
    @GetMapping("/me")
    public ApiResponse<InstagramAccountDto> getMyAccount(
            @RequestAttribute("memberId") Long memberId) {
        InstagramAccountDto account = instagramService.getConnectedAccount(memberId);
        if (account != null) {
            // 보안을 위해 액세스 토큰은 마스킹하거나 제거하여 반환
            account.setAccessToken("********");
        }
        return ApiResponse.ok(account);
    }

    /** 인스타그램 계정 연결 해제 */
    @DeleteMapping("/disconnect")
    public ApiResponse<Void> disconnect(@RequestAttribute("memberId") Long memberId) {
        instagramService.disconnectAccount(memberId);
        return ApiResponse.ok(null);
    }
}