package com.crown.billing.service;

import com.crown.billing.dao.TokenDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private final TokenDao tokenDao;

    private static final DateTimeFormatter YM_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    // ── 토큰 잔액 조회 ──────────────────────────────────────────────

    public Map<String, Object> getBalance(Long memberId) {
        String ym = YearMonth.now().format(YM_FMT);
        Map<String, Object> wallet = tokenDao.getWallet(memberId, ym);
        if (wallet == null) {
            return Map.of("balance", 0, "grantedMonthly", 0, "usedMonthly", 0,
                    "ym", ym, "expiresAt", "");
        }
        return wallet;
    }

    // ── 토큰 충전 (구독 결제 시) ────────────────────────────────────

    @Transactional
    public void grantMonthlyTokens(Long memberId, String planId) {
        Map<String, Object> planConfig = tokenDao.getPlanConfig(planId);
        if (planConfig == null) {
            log.warn("플랜 설정 없음: {}", planId);
            return;
        }

        int tokens = ((Number) planConfig.get("tokensMonthly")).intValue();
        if (tokens <= 0) return;

        String ym = YearMonth.now().format(YM_FMT);
        // 만료일: 다음 달 1일 자정
        LocalDateTime expiresAt = YearMonth.now().plusMonths(1).atDay(1).atStartOfDay();

        // 지갑 upsert
        Map<String, Object> walletParams = new HashMap<>();
        walletParams.put("memberId", memberId);
        walletParams.put("balance", tokens);
        walletParams.put("grantedMonthly", tokens);
        walletParams.put("ym", ym);
        walletParams.put("expiresAt", expiresAt);
        tokenDao.upsertWallet(walletParams);

        // 원장 기록
        Map<String, Object> wallet = tokenDao.getWallet(memberId, ym);
        int balanceAfter = wallet != null ? ((Number) wallet.get("balance")).intValue() : tokens;

        insertLedger(memberId, "grant", tokens, balanceAfter,
                planConfig.get("name") + " 플랜 월 충전", null, expiresAt);

        log.info("토큰 충전: memberId={}, plan={}, tokens={}, expiresAt={}", memberId, planId, tokens, expiresAt);
    }

    // ── 토큰 사용 (AI 기능 호출 시) ─────────────────────────────────

    @Transactional
    public void useTokens(Long memberId, int amount, String description, Long projectId) {
        String ym = YearMonth.now().format(YM_FMT);
        Map<String, Object> wallet = tokenDao.getWallet(memberId, ym);

        int currentBalance = wallet != null ? ((Number) wallet.get("balance")).intValue() : 0;

        if (currentBalance < amount) {
            throw new InsufficientTokenException(
                    "토큰이 부족합니다. (필요: " + amount + ", 잔여: " + currentBalance + ")");
        }

        // 차감
        tokenDao.addUsed(memberId, ym, amount);

        int balanceAfter = currentBalance - amount;
        insertLedger(memberId, "use", -amount, balanceAfter, description, projectId, null);

        log.info("토큰 사용: memberId={}, amount={}, balance={}, desc={}", memberId, amount, balanceAfter, description);
    }

    // ── 토큰 잔액 체크 (차감 없이 확인만) ──────────────────────────

    public boolean hasEnoughTokens(Long memberId, int amount) {
        String ym = YearMonth.now().format(YM_FMT);
        Map<String, Object> wallet = tokenDao.getWallet(memberId, ym);
        int balance = wallet != null ? ((Number) wallet.get("balance")).intValue() : 0;
        return balance >= amount;
    }

    // ── 토큰 환불 (생성 실패 시) ────────────────────────────────────

    @Transactional
    public void refundTokens(Long memberId, int amount, String description, Long projectId) {
        String ym = YearMonth.now().format(YM_FMT);
        tokenDao.addBalance(memberId, ym, amount);

        Map<String, Object> wallet = tokenDao.getWallet(memberId, ym);
        int balanceAfter = wallet != null ? ((Number) wallet.get("balance")).intValue() : amount;

        insertLedger(memberId, "refund", amount, balanceAfter, description, projectId, null);

        log.info("토큰 환불: memberId={}, amount={}, balance={}", memberId, amount, balanceAfter);
    }

    // ── 보너스 토큰 지급 ────────────────────────────────────────────

    @Transactional
    public void grantBonus(Long memberId, int amount, String description) {
        String ym = YearMonth.now().format(YM_FMT);
        LocalDateTime expiresAt = YearMonth.now().plusMonths(1).atDay(1).atStartOfDay();

        // 지갑이 없으면 생성
        Map<String, Object> wallet = tokenDao.getWallet(memberId, ym);
        if (wallet == null) {
            Map<String, Object> walletParams = new HashMap<>();
            walletParams.put("memberId", memberId);
            walletParams.put("balance", amount);
            walletParams.put("grantedMonthly", 0);
            walletParams.put("ym", ym);
            walletParams.put("expiresAt", expiresAt);
            tokenDao.upsertWallet(walletParams);
        } else {
            tokenDao.addBalance(memberId, ym, amount);
        }

        wallet = tokenDao.getWallet(memberId, ym);
        int balanceAfter = wallet != null ? ((Number) wallet.get("balance")).intValue() : amount;

        insertLedger(memberId, "bonus", amount, balanceAfter, description, null, expiresAt);

        log.info("보너스 토큰: memberId={}, amount={}, desc={}", memberId, amount, description);
    }

    // ── 토큰 이력 조회 ──────────────────────────────────────────────

    public List<Map<String, Object>> getHistory(Long memberId, int page, int size) {
        int offset = (page - 1) * size;
        return tokenDao.getLedgerHistory(memberId, size, offset);
    }

    public int getHistoryCount(Long memberId) {
        return tokenDao.getLedgerCount(memberId);
    }

    // ── 플랜 설정 조회 ──────────────────────────────────────────────

    public Map<String, Object> getPlanConfig(String planId) {
        return tokenDao.getPlanConfig(planId);
    }

    public List<Map<String, Object>> getAllPlanConfigs() {
        return tokenDao.getAllPlanConfigs();
    }

    // ── 만료 토큰 처리 (매일 새벽 3시) ──────────────────────────────

    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    @Transactional
    public void processExpiredTokens() {
        List<Map<String, Object>> expired = tokenDao.getExpiredWallets();
        for (Map<String, Object> w : expired) {
            Long memberId = ((Number) w.get("memberId")).longValue();
            int balance = ((Number) w.get("balance")).intValue();
            String ym = (String) w.get("ym");

            if (balance > 0) {
                // 잔액 0으로
                tokenDao.addBalance(memberId, ym, -balance);
                insertLedger(memberId, "expire", -balance, 0,
                        ym + " 미사용 토큰 만료", null, null);
                log.info("토큰 만료: memberId={}, expired={}, ym={}", memberId, balance, ym);
            }
        }
        if (!expired.isEmpty()) {
            log.info("만료 토큰 처리 완료: {}건", expired.size());
        }
    }

    // ── Free 플랜 초기 토큰 지급 (회원가입 시) ──────────────────────

    @Transactional
    public void initFreeTokens(Long memberId) {
        grantMonthlyTokens(memberId, "free");
    }

    // ── 플랜 가격/이름 조회 (PlanLimits.java 대체) ─────────────────

    public int getPlanPrice(String planId, String cycle) {
        Map<String, Object> config = tokenDao.getPlanConfig(planId);
        if (config == null) return 0;
        if ("yearly".equals(cycle)) {
            return ((Number) config.getOrDefault("yearlyPrice", 0)).intValue();
        }
        return ((Number) config.getOrDefault("monthlyPrice", 0)).intValue();
    }

    public String getPlanOrderName(String planId, String cycle) {
        Map<String, Object> config = tokenDao.getPlanConfig(planId);
        String name = config != null ? (String) config.get("name") : planId;
        String cycleName = "yearly".equals(cycle) ? "연간" : "월간";
        return "Velona AI " + name + " 플랜 " + cycleName + " 구독";
    }

    public List<Map<String, Object>> getAllPlans() {
        return tokenDao.getAllPlanConfigs();
    }

    // ── 기능별 토큰 차감 (sm_feature_cost 기반) ─────────────────────

    @Transactional
    public void useTokensForFeature(Long memberId, String featureKey, Long projectId) {
        Map<String, Object> feature = tokenDao.getFeatureCost(featureKey);
        if (feature == null) {
            log.warn("기능 비용 미설정: {}", featureKey);
            return;
        }
        int cost = ((Number) feature.get("tokenCost")).intValue();
        String name = (String) feature.get("name");
        useTokens(memberId, cost, name, projectId);
    }

    // ── 토큰 환불 ──────────────────────────────────────────────────

    @Transactional
    public void refundTokens(Long memberId, int amount, String description) {
        String ym = YearMonth.now().format(YM_FMT);
        tokenDao.addBalance(memberId, ym, amount);
        Map<String, Object> wallet = tokenDao.getWallet(memberId, ym);
        int balanceAfter = wallet != null ? ((Number) wallet.get("balance")).intValue() : amount;
        insertLedger(memberId, "refund", amount, balanceAfter, description, null, null);
        log.info("토큰 환불: memberId={}, amount={}, reason={}", memberId, amount, description);
    }

    // ── 보너스 토큰 지급 (초대 보상 등) ─────────────────────────────

    @Transactional
    public void grantBonus(Long memberId, int amount, String description) {
        String ym = YearMonth.now().format(YM_FMT);
        LocalDateTime expiresAt = YearMonth.now().plusMonths(1).atDay(1).atStartOfDay();

        Map<String, Object> walletParams = new HashMap<>();
        walletParams.put("memberId", memberId);
        walletParams.put("balance", amount);
        walletParams.put("grantedMonthly", 0);
        walletParams.put("ym", ym);
        walletParams.put("expiresAt", expiresAt);
        tokenDao.upsertWallet(walletParams);

        // 기존 잔액에 추가
        tokenDao.addBalance(memberId, ym, amount);

        Map<String, Object> wallet = tokenDao.getWallet(memberId, ym);
        int balanceAfter = wallet != null ? ((Number) wallet.get("balance")).intValue() : amount;
        insertLedger(memberId, "bonus", amount, balanceAfter, description, null, expiresAt);
        log.info("보너스 토큰: memberId={}, amount={}, reason={}", memberId, amount, description);
    }

    // ── 내부 유틸 ───────────────────────────────────────────────────

    private void insertLedger(Long memberId, String type, int amount, int balanceAfter,
                              String description, Long projectId, LocalDateTime expiresAt) {
        Map<String, Object> params = new HashMap<>();
        params.put("memberId", memberId);
        params.put("type", type);
        params.put("amount", amount);
        params.put("balanceAfter", balanceAfter);
        params.put("description", description);
        params.put("projectId", projectId);
        params.put("expiresAt", expiresAt);
        tokenDao.insertLedger(params);
    }
}
