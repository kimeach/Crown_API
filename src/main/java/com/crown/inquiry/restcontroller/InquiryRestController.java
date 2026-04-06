package com.crown.inquiry.restcontroller;

import com.crown.inquiry.service.InquiryService;
import com.crown.member.dto.MemberDto;
import com.crown.member.service.MemberService;
import com.google.firebase.auth.FirebaseToken;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/inquiry")
@RequiredArgsConstructor
public class InquiryRestController {

    private final InquiryService inquiryService;
    private final MemberService memberService;

    /** 로그인 사용자 문의 */
    @PostMapping
    public ResponseEntity<?> submit(
            @AuthenticationPrincipal FirebaseToken token,
            @RequestBody Map<String, String> body) {

        MemberDto member = memberService.findByGoogleId(token.getUid());
        String title   = body.getOrDefault("title", "").trim();
        String content = body.getOrDefault("content", "").trim();

        if (title.isEmpty() || content.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "제목과 내용을 입력해주세요."));
        }
        if (title.length() > 200 || content.length() > 2000) {
            return ResponseEntity.badRequest().body(Map.of("message", "입력 길이를 초과했습니다."));
        }

        inquiryService.submit(member.getMemberId(), member.getNickname(),
                              member.getEmail(), title, content);
        return ResponseEntity.ok(Map.of("message", "문의가 접수되었습니다."));
    }

    /** 비로그인 사용자 문의 (이메일 직접 입력) */
    @PostMapping("/guest")
    public ResponseEntity<?> submitGuest(@RequestBody Map<String, String> body) {
        String email   = body.getOrDefault("email", "").trim();
        String title   = body.getOrDefault("title", "").trim();
        String content = body.getOrDefault("content", "").trim();

        if (email.isEmpty() || title.isEmpty() || content.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "이메일, 제목, 내용을 모두 입력해주세요."));
        }
        if (title.length() > 200 || content.length() > 2000) {
            return ResponseEntity.badRequest().body(Map.of("message", "입력 길이를 초과했습니다."));
        }

        inquiryService.submitGuest(email, title, content);
        return ResponseEntity.ok(Map.of("message", "문의가 접수되었습니다."));
    }
}
