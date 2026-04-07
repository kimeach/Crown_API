package com.crown.common.filter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 모든 API 요청을 access_log 테이블에 기록하는 인터셉터.
 * DB 저장 실패 시 warn만 출력하고 계속 진행.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccessLogInterceptor implements HandlerInterceptor {

    private final JdbcTemplate jdbcTemplate;

    private static final String ATTR_START = "req_start_ms";
    private static final String ATTR_MEMBER = "log_member_id";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(ATTR_START, System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        try {
            long start = (Long) request.getAttribute(ATTR_START);
            int durationMs = (int) (System.currentTimeMillis() - start);

            String path = request.getRequestURI();
            // 내부 콜백, 헬스체크, 정적자원 제외
            if (path.contains("/internal/") || path.equals("/actuator/health")
                    || path.equals("/health")
                    || path.startsWith("/favicon")) return;

            String method = request.getMethod();
            int status     = response.getStatus();
            String ip      = getClientIp(request);
            Long memberId  = (Long) request.getAttribute(ATTR_MEMBER);

            jdbcTemplate.update(
                "INSERT INTO access_log (member_id, path, method, status_code, duration_ms, ip_address) VALUES (?,?,?,?,?,?)",
                memberId, path.substring(0, Math.min(path.length(), 200)),
                method, status, durationMs, ip
            );
        } catch (Exception e) {
            log.warn("[AccessLogInterceptor] 로그 저장 실패: {}", e.getMessage());
        }
    }

    /** member_id 를 인터셉터에서 꺼낼 수 있도록 컨트롤러에서 setAttribute 사용 */
    public static void setMemberId(HttpServletRequest request, Long memberId) {
        request.setAttribute(ATTR_MEMBER, memberId);
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return request.getRemoteAddr();
    }
}
