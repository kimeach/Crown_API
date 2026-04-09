package com.crown.billing.service;

import com.crown.billing.constants.PlanLimits;
import com.crown.billing.constants.UsageLimit;
import com.crown.billing.constants.UsageType;
import com.crown.member.dto.MemberDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * @UsageLimit 어노테이션이 붙은 메서드 호출 전 플랜별 한도 체크.
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class UsageLimitAspect {

    private final BillingService billingService;

    @Before("@annotation(usageLimit)")
    public void checkUsageLimit(JoinPoint jp, UsageLimit usageLimit) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof MemberDto member)) {
            return; // 인증 없으면 패스 (다른 필터에서 차단됨)
        }

        Long memberId = member.getMemberId();
        String plan = billingService.getActivePlan(memberId);
        PlanLimits.Limits limits = PlanLimits.getLimits(plan);

        UsageType type = usageLimit.type();
        int limit = getLimit(limits, type);

        // -1 = 무제한
        if (limit < 0) return;

        Map<String, Object> usage = billingService.getCurrentUsage(memberId);
        int current = usage.containsKey(type.getColumn())
            ? ((Number) usage.get(type.getColumn())).intValue() : 0;

        if (current >= limit) {
            String msg = String.format(
                "%s 플랜의 월간 %s 한도(%d회)를 초과했습니다. 업그레이드해주세요.",
                plan.toUpperCase(), type.name(), limit
            );
            log.warn("[UsageLimit] memberId={}, type={}, current={}, limit={}",
                memberId, type, current, limit);
            throw new UsageLimitExceededException(msg);
        }
    }

    private int getLimit(PlanLimits.Limits limits, UsageType type) {
        return switch (type) {
            case SHORTS -> limits.getShortsMonthly();
            case LONGFORM -> limits.getLongformMonthly();
            case VOICE_CLONE -> limits.getVoiceCloneMonthly();
            case AI_REWRITE -> 50; // AI 재작성은 모든 유료 플랜 월 50회
        };
    }
}
