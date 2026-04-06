package com.crown.common.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class ErrorMonitorScheduler {

    private final JdbcTemplate jdbcTemplate;
    private final RestTemplate restTemplate;

    @Value("${slack.errors.webhook:}")
    private String slackErrorsWebhook;

    // 마지막으로 체크한 시각 (서버 시작 시 현재 시각으로 초기화)
    private LocalDateTime lastChecked = LocalDateTime.now();

    // 5분마다 실행
    @Scheduled(cron = "0 */5 * * * *", zone = "Asia/Seoul")
    public void checkErrors() {
        if (slackErrorsWebhook == null || slackErrorsWebhook.isBlank()) {
            log.debug("[ErrorMonitor] slack.errors.webhook 미설정 — 스킵");
            return;
        }

        LocalDateTime checkFrom = lastChecked;
        lastChecked = LocalDateTime.now();

        try {
            String from = checkFrom.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            List<Map<String, Object>> errors = jdbcTemplate.queryForList(
                "SELECT source, level, path, message, created_at " +
                "FROM error_log " +
                "WHERE created_at > ? " +
                "ORDER BY created_at DESC LIMIT 20",
                from
            );

            if (errors.isEmpty()) return;

            log.info("[ErrorMonitor] 신규 에러 {}건 감지", errors.size());
            sendToSlack(errors, checkFrom);

        } catch (Exception e) {
            log.error("[ErrorMonitor] 오류: {}", e.getMessage());
        }
    }

    private void sendToSlack(List<Map<String, Object>> errors, LocalDateTime since) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("🚨 *신규 에러 ").append(errors.size()).append("건* (")
              .append(since.format(DateTimeFormatter.ofPattern("HH:mm"))).append(" 이후)\n\n");

            // 에러 요약 (최대 5건)
            int shown = Math.min(errors.size(), 5);
            for (int i = 0; i < shown; i++) {
                Map<String, Object> err = errors.get(i);
                sb.append("• `").append(err.getOrDefault("source", "?")).append("` ")
                  .append("*").append(err.getOrDefault("level", "?")).append("*")
                  .append(" — ").append(truncate(String.valueOf(err.getOrDefault("message", "")), 80))
                  .append("\n");
            }
            if (errors.size() > 5) {
                sb.append("... 외 ").append(errors.size() - 5).append("건\n");
            }

            // source별 집계
            Map<String, Long> bySource = new LinkedHashMap<>();
            for (Map<String, Object> err : errors) {
                String src = String.valueOf(err.getOrDefault("source", "unknown"));
                bySource.merge(src, 1L, Long::sum);
            }
            sb.append("\n소스별: ");
            bySource.forEach((k, v) -> sb.append(k).append("(").append(v).append(") "));

            Map<String, String> body = Map.of("text", sb.toString());
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            restTemplate.postForEntity(slackErrorsWebhook, new HttpEntity<>(body, headers), String.class);

        } catch (Exception e) {
            log.error("[ErrorMonitor] Slack 전송 실패: {}", e.getMessage());
        }
    }

    private String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }
}
