package com.crown.friend.restcontroller;

import com.crown.common.dto.ApiResponse;
import com.crown.friend.dto.FriendDto;
import com.crown.friend.service.FriendService;
import com.crown.member.dto.MemberDto;
import com.crown.member.service.MemberService;
import com.google.firebase.auth.FirebaseToken;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/friend")
@RequiredArgsConstructor
public class FriendRestController {

    private final FriendService  friendService;
    private final MemberService  memberService;

    /** 내 친구 목록 */
    @GetMapping("/list")
    public ApiResponse<List<FriendDto>> getFriends(
            @AuthenticationPrincipal FirebaseToken token) {
        MemberDto me = memberService.findByGoogleId(token.getUid());
        return ApiResponse.ok(friendService.getFriends(me.getMemberId()));
    }

    /** 나에게 온 친구 요청 목록 */
    @GetMapping("/requests")
    public ApiResponse<List<FriendDto>> getRequests(
            @AuthenticationPrincipal FirebaseToken token) {
        MemberDto me = memberService.findByGoogleId(token.getUid());
        return ApiResponse.ok(friendService.getPendingRequests(me.getMemberId()));
    }

    /** 친구 요청 전송 */
    @PostMapping("/request/{targetId}")
    public ApiResponse<Void> sendRequest(
            @AuthenticationPrincipal FirebaseToken token,
            @PathVariable Long targetId) {
        MemberDto me = memberService.findByGoogleId(token.getUid());
        friendService.sendRequest(me.getMemberId(), targetId);
        return ApiResponse.ok(null);
    }

    /** 친구 요청 수락 */
    @PostMapping("/accept/{requesterId}")
    public ApiResponse<Void> accept(
            @AuthenticationPrincipal FirebaseToken token,
            @PathVariable Long requesterId) {
        MemberDto me = memberService.findByGoogleId(token.getUid());
        friendService.accept(requesterId, me.getMemberId());
        return ApiResponse.ok(null);
    }

    /** 친구 요청 거절 / 친구 삭제 */
    @DeleteMapping("/{friendId}")
    public ApiResponse<Void> delete(
            @AuthenticationPrincipal FirebaseToken token,
            @PathVariable Long friendId) {
        MemberDto me = memberService.findByGoogleId(token.getUid());
        friendService.removeFriend(me.getMemberId(), friendId);
        return ApiResponse.ok(null);
    }
}
