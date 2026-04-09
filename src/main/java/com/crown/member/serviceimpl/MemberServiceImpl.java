package com.crown.member.serviceimpl;

import com.crown.billing.service.TokenService;
import com.crown.member.dao.MemberDao;
import com.crown.member.dto.FirebaseAttributes;
import com.crown.member.dto.MemberDto;
import com.crown.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private final MemberDao memberDao;
    private final TokenService tokenService;

    @Override
    public MemberDto saveOrUpdate(FirebaseAttributes attributes) {
        MemberDto existing = memberDao.findByGoogleId(attributes.getGoogleId());
        if (existing == null && attributes.getEmail() != null) {
            // google_id로 못 찾으면 email로 fallback (Firebase UID 변경 시 중복 방지)
            existing = memberDao.findByEmail(attributes.getEmail());
            if (existing != null) {
                // 기존 계정에 새 google_id 업데이트
                memberDao.updateGoogleId(existing.getMemberId(), attributes.getGoogleId());
            }
        }
        if (existing == null) {
            memberDao.insert(attributes);
            // 신규 회원 — Free 플랜 초기 토큰 지급
            MemberDto newMember = memberDao.findByGoogleId(attributes.getGoogleId());
            if (newMember != null) {
                try {
                    tokenService.initFreeTokens(newMember.getMemberId());
                } catch (Exception e) {
                    log.warn("초기 토큰 지급 실패 (무시): {}", e.getMessage());
                }
            }
            return newMember;
        } else {
            memberDao.updateProfile(attributes);
        }
        return memberDao.findByGoogleId(attributes.getGoogleId());
    }

    @Override
    public MemberDto findByGoogleId(String googleId) {
        return memberDao.findByGoogleId(googleId);
    }

    @Override
    public MemberDto findById(Long memberId) {
        return memberDao.findById(memberId);
    }

    @Override
    public List<MemberDto> findRanking(int limit) {
        return memberDao.findRanking(limit);
    }

    @Override
    public List<MemberDto> searchByNickname(String nickname, Long excludeId) {
        return memberDao.searchByNickname(nickname, excludeId);
    }

    @Override
    public MemberDto updateNickname(Long memberId, String nickname) {
        memberDao.updateNickname(memberId, nickname);
        return memberDao.findById(memberId);
    }

    @Override
    public MemberDto updateProfileImg(Long memberId, String profileImg) {
        memberDao.updateProfileImg(memberId, profileImg);
        return memberDao.findById(memberId);
    }

    @Override
    public void updateFcmToken(Long memberId, String fcmToken) {
        memberDao.updateFcmToken(memberId, fcmToken);
    }

    @Override
    public String getFcmToken(Long memberId) {
        return memberDao.getFcmToken(memberId);
    }
}
