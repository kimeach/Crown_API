package com.crown.member.service;

import com.crown.member.dto.FirebaseAttributes;
import com.crown.member.dto.MemberDto;
import java.util.List;

public interface MemberService {
    MemberDto saveOrUpdate(FirebaseAttributes attributes);
    MemberDto findByGoogleId(String googleId);
    MemberDto findById(Long memberId);
    List<MemberDto> findRanking(int limit);
    List<MemberDto> searchByNickname(String nickname, Long excludeId);
    MemberDto updateNickname(Long memberId, String nickname);
    MemberDto updateProfileImg(Long memberId, String profileImg);
}
