package com.crown.billing.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface TokenDao {

    // ── 지갑 ──
    Map<String, Object> getWallet(@Param("memberId") Long memberId, @Param("ym") String ym);

    void upsertWallet(Map<String, Object> params);

    void addBalance(@Param("memberId") Long memberId, @Param("ym") String ym, @Param("amount") int amount);

    void addUsed(@Param("memberId") Long memberId, @Param("ym") String ym, @Param("amount") int amount);

    // ── 원장 ──
    void insertLedger(Map<String, Object> params);

    List<Map<String, Object>> getLedgerHistory(@Param("memberId") Long memberId, @Param("limit") int limit, @Param("offset") int offset);

    int getLedgerCount(@Param("memberId") Long memberId);

    // ── 플랜 설정 ──
    Map<String, Object> getPlanConfig(@Param("planId") String planId);

    List<Map<String, Object>> getAllPlanConfigs();

    // ── 기능별 토큰 비용 ──
    Map<String, Object> getFeatureCost(@Param("featureKey") String featureKey);

    // ── 토큰 패키지 ──
    List<Map<String, Object>> getTokenPackages();

    Map<String, Object> getTokenPackageById(@Param("id") Long id);

    // ── 만료 임박 조회 ──
    Map<String, Object> getExpiringWallet(@Param("memberId") Long memberId);

    // ── 만료 처리 ──
    List<Map<String, Object>> getExpiredWallets();
}
