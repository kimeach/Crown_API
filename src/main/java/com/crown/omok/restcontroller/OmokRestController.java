package com.crown.omok.restcontroller;

import com.crown.common.dto.ApiResponse;
import com.crown.member.dto.MemberDto;
import com.crown.member.service.MemberService;
import com.crown.omok.dto.GameRoomDto;
import com.crown.omok.service.OmokService;
import com.google.firebase.auth.FirebaseToken;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/omok")
@RequiredArgsConstructor
public class OmokRestController {

    private final OmokService omokService;
    private final MemberService memberService;

    @GetMapping("/history")
    public ApiResponse<List<GameRoomDto>> getHistory(@AuthenticationPrincipal FirebaseToken firebaseToken) {
        MemberDto member = memberService.findByGoogleId(firebaseToken.getUid());
        return ApiResponse.ok(omokService.findHistory(member.getMemberId()));
    }

    @GetMapping("/room/{roomId}")
    public ApiResponse<GameRoomDto> getRoom(@PathVariable Long roomId) {
        return ApiResponse.ok(omokService.findRoom(roomId));
    }
}
