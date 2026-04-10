package com.crown.billing.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.Map;

/**
 * @deprecated sm_plan_config DB 테이블로 대체됨. TokenService.getPlanPrice(), getPlanOrderName() 사용.
 * UsageLimitAspect에서 아직 참조 중이므로 완전 삭제는 보류.
 */
@Deprecated
public class PlanLimits {

    @Getter
    @AllArgsConstructor
    public static class Limits {
        private final int shortsMonthly;
        private final int longformMonthly;
        private final int voiceCloneMonthly;
        private final int teamMembers;
    }

    public static final Map<String, Limits> PLANS = Map.of(
        "free",     new Limits(5,    0,   0,   1),
        "pro",      new Limits(30,   5,   1,   1),
        "team",     new Limits(100,  20,  3,   5),
        "business", new Limits(500,  -1,  10,  20)  // -1 = 무제한
    );

    /** 플랜별 월간 가격 (원) */
    public static final Map<String, Integer> MONTHLY_PRICES = Map.of(
        "free", 0,
        "pro", 12900,
        "team", 39000,
        "business", 49000
    );

    /** 플랜별 연간 가격 (원) */
    public static final Map<String, Integer> YEARLY_PRICES = Map.of(
        "free", 0,
        "pro", 124000,
        "team", 374000,
        "business", 470000
    );

    public static Limits getLimits(String plan) {
        return PLANS.getOrDefault(plan, PLANS.get("free"));
    }

    public static int getPrice(String plan, String cycle) {
        if ("yearly".equals(cycle)) {
            return YEARLY_PRICES.getOrDefault(plan, 0);
        }
        return MONTHLY_PRICES.getOrDefault(plan, 0);
    }

    public static String getOrderName(String plan, String cycle) {
        String planName = switch (plan) {
            case "pro" -> "Pro";
            case "team" -> "Team";
            case "business" -> "Business";
            default -> "Free";
        };
        String cycleName = "yearly".equals(cycle) ? "연간" : "월간";
        return "Velona AI " + planName + " 플랜 " + cycleName + " 구독";
    }
}
