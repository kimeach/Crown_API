package com.crown.common.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 간단한 인메모리 Rate Limiter.
 * AI 관련 고비용 엔드포인트에 분당 / 시간당 제한 적용.
 *
 * 향후 Redis + Bucket4j로 교체 가능.
 */
@Slf4j
@Component
public class RateLimitFilter implements Filter {

    /** 제한 대상 경로 패턴 */
    private static final Set<String> RATE_LIMITED_PATHS = Set.of(
        "/api/shorts/ai-rewrite",
        "/api/shorts/ai-translate",
        "/api/shorts/ai-hashtags",
        "/api/shorts/ai-seo",
        "/api/shorts/ai-quality"
    );

    private static final int MAX_PER_MINUTE = 10;
    private static final int MAX_PER_HOUR = 100;

    /** key = "ip:path" 또는 "memberId:path" */
    private final Map<String, RateBucket> buckets = new ConcurrentHashMap<>();

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpReq = (HttpServletRequest) req;
        String path = httpReq.getRequestURI();

        // 제한 대상인지 확인
        boolean limited = RATE_LIMITED_PATHS.stream().anyMatch(path::startsWith);
        if (!limited) {
            chain.doFilter(req, res);
            return;
        }

        // 키 생성 (IP 기반, 인증 사용자는 UID 사용)
        String clientKey = extractClientKey(httpReq) + ":" + path;

        RateBucket bucket = buckets.computeIfAbsent(clientKey, k -> new RateBucket());
        bucket.cleanup();

        if (bucket.getMinuteCount() >= MAX_PER_MINUTE) {
            reject(httpReq, (HttpServletResponse) res, "분당 요청 한도 초과 (최대 " + MAX_PER_MINUTE + "회)");
            return;
        }
        if (bucket.getHourCount() >= MAX_PER_HOUR) {
            reject(httpReq, (HttpServletResponse) res, "시간당 요청 한도 초과 (최대 " + MAX_PER_HOUR + "회)");
            return;
        }

        bucket.record();
        chain.doFilter(req, res);
    }

    private String extractClientKey(HttpServletRequest req) {
        // Firebase UID가 있으면 사용, 없으면 IP
        String uid = req.getHeader("X-Firebase-UID");
        if (uid != null && !uid.isBlank()) return uid;
        String forwarded = req.getHeader("X-Forwarded-For");
        return forwarded != null ? forwarded.split(",")[0].trim() : req.getRemoteAddr();
    }

    private void reject(HttpServletRequest req, HttpServletResponse res, String message)
            throws IOException {
        log.warn("[RateLimit] 차단: {} {}", req.getMethod(), req.getRequestURI());
        res.setStatus(429);
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write("{\"success\":false,\"message\":\"" + message + "\"}");
    }

    /** 간단한 슬라이딩 윈도우 카운터 */
    private static class RateBucket {
        private final ConcurrentHashMap<Long, AtomicInteger> timestamps = new ConcurrentHashMap<>();

        void record() {
            long second = System.currentTimeMillis() / 1000;
            timestamps.computeIfAbsent(second, k -> new AtomicInteger(0)).incrementAndGet();
        }

        int getMinuteCount() {
            long now = System.currentTimeMillis() / 1000;
            return timestamps.entrySet().stream()
                .filter(e -> e.getKey() >= now - 60)
                .mapToInt(e -> e.getValue().get())
                .sum();
        }

        int getHourCount() {
            long now = System.currentTimeMillis() / 1000;
            return timestamps.entrySet().stream()
                .filter(e -> e.getKey() >= now - 3600)
                .mapToInt(e -> e.getValue().get())
                .sum();
        }

        void cleanup() {
            long cutoff = System.currentTimeMillis() / 1000 - 3600;
            timestamps.entrySet().removeIf(e -> e.getKey() < cutoff);
        }
    }
}
