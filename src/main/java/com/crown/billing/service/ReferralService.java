package com.crown.billing.service;

import com.crown.billing.dao.ReferralDao;
import com.crown.member.dao.MemberDao;
import com.crown.member.dto.MemberDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReferralService {

    private final ReferralDao referralDao;
    private final MemberDao memberDao;
    private final TokenService tokenService;

    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    // ── 초대 코드 생성/조회 ────────────────────────────────────────────

    @Transactional
    public String generateCode(Long memberId) {
        MemberDto member = memberDao.findById(memberId);
        if (member == null) {
            throw new IllegalArgumentException("회원을 찾을 수 없습니다");
        }

        // 이미 코드가 있으면 반환
        String existingCode = memberDao.findReferralCode(memberId);
        if (existingCode != null && !existingCode.isEmpty()) {
            return existingCode;
        }

        // 유니크 코드 생성 (충돌 시 최대 10회 재시도)
        String code = null;
        for (int i = 0; i < 10; i++) {
            code = generateRandomCode();
            MemberDto existing = memberDao.findByReferralCode(code);
            if (existing == null) {
                break;
            }
            if (i == 9) {
                throw new IllegalStateException("초대 코드 생성 실패: 중복 코드");
            }
        }

        memberDao.updateReferralCode(memberId, code);
        log.info("[Referral] 초대 코드 생성: memberId={}, code={}", memberId, code);
        return code;
    }

    // ── 코드 유효성 검증 ───────────────────────────────────────────────

    public Map<String, Object> validateCode(String code) {
        if (code == null || code.length() != CODE_LENGTH) {
            return Map.of("valid", false, "message", "유효하지 않은 코드 형식입니다");
        }

        MemberDto inviter = memberDao.findByReferralCode(code);
        if (inviter == null) {
            return Map.of("valid", false, "message", "존재하지 않는 초대 코드입니다");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("valid", true);
        result.put("inviterNickname", inviter.getNickname());
        result.put("inviterProfileImg", inviter.getProfileImg());
        result.put("message", inviter.getNickname() + "님의 초대 코드입니다");
        return result;
    }

    // ── 가입 보너스 처리 ───────────────────────────────────────────────

    @Transactional
    public void processSignupBonus(Long inviteeId, String referralCode) {
        if (referralCode == null || referralCode.isEmpty()) {
            return;
        }

        MemberDto inviter = memberDao.findByReferralCode(referralCode);
        if (inviter == null) {
            log.warn("[Referral] 유효하지 않은 초대 코드: code={}", referralCode);
            return;
        }

        // 자기 자신 초대 방지
        if (inviter.getMemberId().equals(inviteeId)) {
            log.warn("[Referral] 자기 자신 초대 시도: memberId={}", inviteeId);
            return;
        }

        // 이미 초대 관계가 있는지 확인
        Map<String, Object> existing = referralDao.getByInviterAndInvitee(
                inviter.getMemberId(), inviteeId);
        if (existing != null) {
            log.warn("[Referral] 이미 초대 관계 존재: inviter={}, invitee={}",
                    inviter.getMemberId(), inviteeId);
            return;
        }

        // 보상 설정 조회
        Map<String, Object> rewardConfig = referralDao.getRewardConfig("signup", "all");
        int inviterReward = 50;
        int inviteeReward = 50;
        if (rewardConfig != null) {
            inviterReward = ((Number) rewardConfig.get("inviter_reward")).intValue();
            inviteeReward = ((Number) rewardConfig.get("invitee_reward")).intValue();
        }

        // 초대 레코드 생성
        Map<String, Object> params = new HashMap<>();
        params.put("inviterId", inviter.getMemberId());
        params.put("inviteeId", inviteeId);
        params.put("referralCode", referralCode);
        params.put("status", "completed");
        referralDao.insertReferral(params);

        // 가입 보너스 지급 (쌍방)
        tokenService.grantBonus(inviter.getMemberId(), inviterReward,
                "친구 초대 보너스 (가입)");
        tokenService.grantBonus(inviteeId, inviteeReward,
                "초대 코드 가입 보너스");

        // 보너스 지급 플래그 업데이트
        Map<String, Object> referral = referralDao.getByInviterAndInvitee(
                inviter.getMemberId(), inviteeId);
        if (referral != null) {
            referralDao.updateSignupBonusGiven(((Number) referral.get("id")).longValue());
        }

        log.info("[Referral] 가입 보너스 지급: inviter={} (+{}), invitee={} (+{})",
                inviter.getMemberId(), inviterReward, inviteeId, inviteeReward);
    }

    // ── 구독 보너스 처리 ───────────────────────────────────────────────

    @Transactional
    public void processSubscriptionBonus(Long memberId, String plan) {
        // 이 회원을 초대한 레코드 찾기
        Map<String, Object> referral = referralDao.getReferralByInviteeId(memberId);
        if (referral == null) {
            return; // 초대로 가입한 회원이 아님
        }

        // 이미 구독 보너스를 지급했는지 확인
        if (referral.get("sub_bonus_given") != null
                && ((Number) referral.get("sub_bonus_given")).intValue() == 1) {
            log.info("[Referral] 이미 구독 보너스 지급됨: invitee={}", memberId);
            return;
        }

        // 보상 설정 조회
        Map<String, Object> rewardConfig = referralDao.getRewardConfig("subscription", plan);
        if (rewardConfig == null) {
            rewardConfig = referralDao.getRewardConfig("subscription", "all");
        }
        if (rewardConfig == null) {
            log.info("[Referral] 구독 보상 설정 없음: plan={}", plan);
            return;
        }

        int inviterReward = ((Number) rewardConfig.get("inviter_reward")).intValue();
        Long inviterId = ((Number) referral.get("inviter_id")).longValue();
        Long referralId = ((Number) referral.get("id")).longValue();

        // 초대자에게 보너스 지급
        tokenService.grantBonus(inviterId, inviterReward,
                "친구 구독 보너스 (" + plan + " 플랜)");

        // 구독 보너스 지급 플래그 업데이트
        referralDao.updateSubBonusGiven(referralId);

        log.info("[Referral] 구독 보너스 지급: inviter={} (+{}), invitee={}, plan={}",
                inviterId, inviterReward, memberId, plan);
    }

    // ── 내 초대 통계 ───────────────────────────────────────────────────

    public Map<String, Object> getMyStats(Long memberId) {
        Map<String, Object> raw = referralDao.getStatsByInviter(memberId);
        Map<String, Object> stats = new HashMap<>();
        if (raw != null) {
            stats.put("totalInvited", raw.getOrDefault("total_invited", 0));
            stats.put("signedUp", raw.getOrDefault("completed_count", 0));
            stats.put("subscribed", raw.getOrDefault("sub_bonus_count", 0));
        } else {
            stats.put("totalInvited", 0);
            stats.put("signedUp", 0);
            stats.put("subscribed", 0);
        }

        String code = memberDao.findReferralCode(memberId);
        stats.put("referralCode", code);
        return stats;
    }

    // ── 초대 이력 ──────────────────────────────────────────────────────

    public Map<String, Object> getHistory(Long memberId, int page, int size) {
        int offset = (page - 1) * size;
        List<Map<String, Object>> list = referralDao.getHistoryByInviter(memberId, size, offset);
        int total = referralDao.getHistoryCountByInviter(memberId);

        Map<String, Object> result = new HashMap<>();
        result.put("content", list);
        result.put("totalPages", (int) Math.ceil((double) total / size));
        result.put("totalElements", total);
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    // ── 내부 유틸 ──────────────────────────────────────────────────────

    private String generateRandomCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
        }
        return sb.toString();
    }
}
