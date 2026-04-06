package com.crown.common.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
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

    @Value("${google.sheets.webhook:}")
    private String sheetsWebhook;

    @Value("${google.sheets.token:}")
    private String sheetsToken;

    private LocalDateTime lastChecked = LocalDateTime.now();

    @Scheduled(cron = "0 */5 * * * *", zone = "Asia/Seoul")
    public void checkErrors() {
        if (slackErrorsWebhook == null || slackErrorsWebhook.isBlank()) return;

        LocalDateTime checkFrom = lastChecked;
        lastChecked = LocalDateTime.now();

        try {
            String from = checkFrom.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            List<Map<String, Object>> errors = jdbcTemplate.queryForList(
                "SELECT log_id, source, level, path, message, created_at " +
                "FROM error_log " +
                "WHERE created_at > ? " +
                "ORDER BY created_at DESC LIMIT 20",
                from
            );

            if (errors.isEmpty()) return;

            log.info("[ErrorMonitor] 신규 에러 {}건 감지", errors.size());
            sendToSlack(errors, checkFrom);
            recordToSheets(errors);

        } catch (Exception e) {
            log.error("[ErrorMonitor] 오류: {}", e.getMessage());
        }
    }

    private void sendToSlack(List<Map<String, Object>> errors, LocalDateTime since) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("🚨 *신규 에러 ").append(errors.size()).append("건* (")
              .append(since.format(DateTimeFormatter.ofPattern("HH:mm"))).append(" 이후)\n\n");

            int shown = Math.min(errors.size(), 5);
            for (int i = 0; i < shown; i++) {
                Map<String, Object> err = errors.get(i);
                // log_id를 메시지에 포함 (CCR이 Sheets 업데이트 시 사용)
                sb.append("• `").append(err.getOrDefault("source", "?")).append("` ")
                  .append("*").append(err.getOrDefault("level", "?")).append("*")
                  .append(" [log_id:").append(err.getOrDefault("log_id", "?")).append("]")
                  .append(" — ").append(truncate(String.valueOf(err.getOrDefault("message", "")), 80))
                  .append("\n");
            }
            if (errors.size() > 5) {
                sb.append("... 외 ").append(errors.size() - 5).append("건\n");
            }

            Map<String, Long> bySource = new LinkedHashMap<>();
            for (Map<String, Object> err : errors) {
                String src = String.valueOf(err.getOrDefault("source", "unknown"));
                bySource.merge(src, 1L, Long::sum);
            }
            sb.append("\n소스별: ");
            bySource.forEach((k, v) -> sb.append(k).append("(").append(v).append(") "));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            restTemplate.postForEntity(slackErrorsWebhook,
                new HttpEntity<>(Map.of("text", sb.toString()), headers), String.class);

        } catch (Exception e) {
            log.error("[ErrorMonitor] Slack 전송 실패: {}", e.getMessage());
        }
    }

    private void recordToSheets(List<Map<String, Object>> errors) {
        if (sheetsWebhook == null || sheetsWebhook.isBlank()) return;
        for (Map<String, Object> err : errors) {
            try {
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("token",   sheetsToken);
                payload.put("type",    "error_log");
                payload.put("action",  "insert");
                payload.put("log_id",  String.valueOf(err.getOrDefault("log_id", "")));
                payload.put("date",    LocalDate.now().toString());
                payload.put("source",  err.getOrDefault("source", ""));
                payload.put("level",   err.getOrDefault("level", ""));
                payload.put("message", truncate(String.valueOf(err.getOrDefault("message", "")), 200));
                payload.put("status",  "감지됨");
                payload.put("fix_description", "");

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                restTemplate.postForEntity(sheetsWebhook, new HttpEntity<>(payload, headers), String.class);
            } catch (Exception e) {
                log.warn("[ErrorMonitor] Sheets 기록 실패: {}", e.getMessage());
            }
        }
    }

    private String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }
}
