package com.crown.member.dao;

import com.crown.member.dto.FirebaseAttributes;
import com.crown.member.dto.MemberDto;
import com.crown.member.mapper.MemberMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class MemberDao {

    private final MemberMapper memberMapper;

    public MemberDto findByGoogleId(String googleId) {
        return memberMapper.findByGoogleId(googleId);
    }

    public MemberDto findByEmail(String email) {
        return memberMapper.findByEmail(email);
    }

    public void updateGoogleId(Long memberId, String googleId) {
        memberMapper.updateGoogleId(memberId, googleId);
    }

    public MemberDto findById(Long memberId) {
        return memberMapper.findById(memberId);
    }

    public void insert(FirebaseAttributes attributes) {
        memberMapper.insert(attributes);
    }

    public void updateProfile(FirebaseAttributes attributes) {
        memberMapper.updateProfile(attributes);
    }

    public void updateNickname(Long memberId, String nickname) {
        memberMapper.updateNickname(memberId, nickname);
    }

    public void updateProfileImg(Long memberId, String profileImg) {
        memberMapper.updateProfileImg(memberId, profileImg);
    }

    public void updateFcmToken(Long memberId, String fcmToken) {
        memberMapper.updateFcmToken(memberId, fcmToken);
    }

    public String getFcmToken(Long memberId) {
        return memberMapper.getFcmToken(memberId);
    }

    public void updateScore(Long memberId, int scoreDelta, String result) {
        memberMapper.updateScore(memberId, scoreDelta, result);
    }

    public List<MemberDto> findRanking(int limit) {
        return memberMapper.findRanking(limit);
    }

    public List<MemberDto> searchByNickname(String nickname, Long excludeId) {
        return memberMapper.searchByNickname(nickname, excludeId);
    }

    public void updateReferralCode(Long memberId, String referralCode) {
        memberMapper.updateReferralCode(memberId, referralCode);
    }

    public MemberDto findByReferralCode(String referralCode) {
        return memberMapper.findByReferralCode(referralCode);
    }

    public String findReferralCode(Long memberId) {
        return memberMapper.findReferralCodeByMemberId(memberId);
    }
}
