package com.crown.shorts.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsageLimitService {

    private final UsageMapper usageMapper;

    public static final int FREE_MONTHLY_LIMIT = 5;  // 무료: 월 5회

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

    @Mapper
    public interface UsageMapper {
        @Select("SELECT COUNT(*) FROM sm_usage_log " +
                "WHERE member_id = #{memberId} AND action = #{action} " +
                "AND DATE(created_at) BETWEEN #{from} AND #{to}")
        int countThisMonth(@Param("memberId") Long memberId,
                           @Param("action") String action,
                           @Param("from") String from,
                           @Param("to") String to);

        @Insert("INSERT INTO sm_usage_log (member_id, action) VALUES (#{memberId}, #{action})")
        void insert(@Param("memberId") Long memberId, @Param("action") String action);
    }
}
