package com.crown.shorts.service;

import com.crown.shorts.mapper.UsageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.YearMonth;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsageLimitService {

    private final UsageMapper usageMapper;

    public static final int FREE_MONTHLY_LIMIT = 10000;  // 무료: 월 10000회

    /**
     * 이번 달 사용량 확인 후 초과 시 예외 발생
     */
    public void checkAndRecord(Long memberId, String plan, String action) {
        if ("pro".equalsIgnoreCase(plan)) {
            usageMapper.insert(memberId, action);
            return;
        }
        YearMonth now = YearMonth.now();
        int count = usageMapper.countThisMonth(memberId, action,
                now.atDay(1).toString(), now.atEndOfMonth().toString());
        if (count >= FREE_MONTHLY_LIMIT) {
            throw new IllegalStateException(
                String.format("무료 플랜 월 사용량(%d회)을 초과했습니다. Pro로 업그레이드하세요.", FREE_MONTHLY_LIMIT));
        }
        usageMapper.insert(memberId, action);
        log.info("[Usage] member={} action={} 이번달 {}회 → {}회", memberId, action, count, count + 1);
    }

    public int getMonthlyUsage(Long memberId, String action) {
        YearMonth now = YearMonth.now();
        return usageMapper.countThisMonth(memberId, action,
                now.atDay(1).toString(), now.atEndOfMonth().toString());
    }
}
