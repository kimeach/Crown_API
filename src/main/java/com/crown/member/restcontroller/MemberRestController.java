package com.crown.member.restcontroller;

import com.crown.common.dto.ApiResponse;
import com.crown.member.dto.FirebaseAttributes;
import com.crown.member.dto.MemberDto;
import com.crown.member.service.MemberService;
import com.google.firebase.auth.FirebaseToken;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
public class MemberRestController {

    private final MemberService memberService;

    // Firebase 로그인 후 최초 1회 호출 → 회원 생성 또는 프로필 갱신
    @PostMapping("/login")
    public ApiResponse<MemberDto> login(@AuthenticationPrincipal FirebaseToken firebaseToken) {
        FirebaseAttributes attributes = FirebaseAttributes.of(firebaseToken);
        MemberDto member = memberService.saveOrUpdate(attributes);
        return ApiResponse.ok(member);
    }

    // 내 정보 조회
    @GetMapping("/me")
    public ApiResponse<MemberDto> getMe(@AuthenticationPrincipal FirebaseToken firebaseToken) {
        MemberDto member = memberService.findByGoogleId(firebaseToken.getUid());
        return ApiResponse.ok(member);
    }

    // 닉네임 변경
    @PutMapping("/nickname")
    public ApiResponse<MemberDto> updateNickname(
            @AuthenticationPrincipal FirebaseToken firebaseToken,
            @RequestBody Map<String, String> body) {
        MemberDto me = memberService.findByGoogleId(firebaseToken.getUid());
        MemberDto updated = memberService.updateNickname(me.getMemberId(), body.get("nickname"));
        return ApiResponse.ok(updated);
    }

    // 프로필 사진 변경
    @PutMapping("/profile-image")
    public ApiResponse<MemberDto> updateProfileImage(
            @AuthenticationPrincipal FirebaseToken firebaseToken,
            @RequestBody Map<String, String> body) {
        MemberDto me = memberService.findByGoogleId(firebaseToken.getUid());
        MemberDto updated = memberService.updateProfileImg(me.getMemberId(), body.get("profileImg"));
        return ApiResponse.ok(updated);
    }

    // 닉네임으로 회원 검색
    @GetMapping("/search")
    public ApiResponse<List<MemberDto>> search(
            @AuthenticationPrincipal FirebaseToken firebaseToken,
            @RequestParam String nickname) {
        MemberDto me = memberService.findByGoogleId(firebaseToken.getUid());
        List<MemberDto> results = memberService.searchByNickname(nickname, me.getMemberId());
        return ApiResponse.ok(results);
    }
}
