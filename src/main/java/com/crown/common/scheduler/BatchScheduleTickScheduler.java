package com.crown.common.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * 배치 스케줄 틱 — 매분 Python 워커 /schedule/tick 호출
 * 실행 예정 스케줄을 Python 워커가 처리(TTS+렌더+Slack 알림)하도록 트리거.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BatchScheduleTickScheduler {

    private final RestTemplate restTemplate;

    @Value("${worker.url:http://localhost:8003}")
    private String workerUrl;

    @Value("${worker.secret:change-me-in-production}")
    private String workerSecret;

    @Scheduled(cron = "0 * * * * *", zone = "Asia/Seoul")
    public void tick() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Worker-Secret", workerSecret);

            ResponseEntity<Map> response = restTemplate.exchange(
                workerUrl + "/schedule/tick",
                HttpMethod.POST,
                new HttpEntity<>(null, headers),
                Map.class
            );

            Map<?, ?> body = response.getBody();
            if (body == null) return;

            Object triggered = body.get("triggered");
            if (triggered instanceof List && !((List<?>) triggered).isEmpty()) {
                log.info("[BatchScheduleTick] 실행 트리거 {}건: {}",
                    ((List<?>) triggered).size(), triggered);
            }

        } catch (Exception e) {
            log.warn("[BatchScheduleTick] tick 실패 (무시): {}", e.getMessage());
        }
    }
}
