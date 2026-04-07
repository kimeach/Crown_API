package com.crown.common.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class FeatureProposalScheduler {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;

    @Value("${worker.url}")
    private String workerUrl;

    @Value("${worker.secret}")
    private String workerSecret;

    @Value("${slack.ideas.webhook}")
    private String slackIdeasWebhook;

    @Value("${gemini.api-key:}")
    private String geminiApiKey;

    // 매일 오전 8시, 오후 7시 실행 (KST)
    @Scheduled(cron = "0 0 8,19 * * *", zone = "Asia/Seoul")
    public void runDailyFeatureProposal() {
        log.info("[FeatureProposal] 일일 기능 제안 시작");
        try {
            // Step 1: 트렌딩 토픽 수집
            String trendingContext = fetchTrendingTopics();

            // Step 2: Gemini로 기능 제안 생성
            List<Map<String, String>> proposals = generateProposals(trendingContext);
            if (proposals.isEmpty()) {
                log.warn("[FeatureProposal] 제안 생성 실패 — 종료");
                return;
            }

            // Step 3: Slack 전송
            sendToSlack(proposals);

            // Step 4: DB 저장
            recordToDb(proposals);

            log.info("[FeatureProposal] 완료 — {}개 제안 저장", proposals.size());
        } catch (Exception e) {
            log.error("[FeatureProposal] 오류: {}", e.getMessage(), e);
        }
    }

    private String fetchTrendingTopics() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Worker-Secret", workerSecret);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<String> res = restTemplate.exchange(
                workerUrl + "/trending/topics?category=stock&limit=5",
                HttpMethod.GET, entity, String.class);
            return res.getBody() != null ? res.getBody() : "";
        } catch (Exception e) {
            log.warn("[FeatureProposal] 트렌딩 토픽 수집 실패: {}", e.getMessage());
            return "";
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, String>> generateProposals(String trendingContext) {
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            log.warn("[FeatureProposal] GEMINI_API_KEY 미설정 — 폴백 제안 사용");
            return fallbackProposals();
        }
        try {
            String prompt = buildPrompt(trendingContext);
            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + geminiApiKey;

            Map<String, Object> body = Map.of(
                "contents", List.of(Map.of(
                    "parts", List.of(Map.of("text", prompt))
                ))
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<Map> res = restTemplate.exchange(
                url, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);

            if (res.getBody() == null) return fallbackProposals();

            List<Map<String, Object>> candidates = (List<Map<String, Object>>) res.getBody().get("candidates");
            if (candidates == null || candidates.isEmpty()) return fallbackProposals();

            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            String text = (String) parts.get(0).get("text");

            return parseProposals(text);
        } catch (Exception e) {
            log.warn("[FeatureProposal] Gemini 호출 실패: {}", e.getMessage());
            return fallbackProposals();
        }
    }

    private String buildPrompt(String trendingContext) {
        return """
            당신은 Velona AI (YouTube Shorts 자동 생성 SaaS)의 제품 기획자입니다.
            경쟁사(Vrew, CapCut, InVideo, Typecast)를 분석하여 Velona AI에 없는 유용한 기능 3개를 제안해주세요.

            이미 구현된 기능 (제안 금지):
            YouTube Shorts 자동 생성, 영상 편집기, TTS, FFmpeg, PDF/PPT 내보내기,
            프로젝트 복제, AI 해시태그/SEO/품질 분석, 썸네일, 트렌딩 토픽, 버전 히스토리, Firebase, AWS S3

            트렌딩 컨텍스트: %s

            각 제안은 반드시 아래 형식으로 작성해주세요:
            ---
            기능명: [기능 이름]
            참고서비스: [Vrew/CapCut/InVideo/Typecast 중 하나]
            구현방법: [간단한 구현 설명 1~2줄]
            난이도: [낮음/중간/높음]
            예상시간: [예: 2~3시간]
            자동개발: [가능/불가능]
            자동개발불가이유: [자동개발이 불가능한 경우에만 한 줄로 이유 작성, 가능하면 빈칸]
            ---

            자동개발 가능 기준 (모두 충족 시 가능):
            - 난이도 낮음 또는 중간
            - 기존 파일 수정만으로 구현 가능 (새 외부 API 연동 없음)
            - DB 스키마 변경 없음
            - 예상 수정 범위 50줄 이하

            자동개발 불가 기준 (하나라도 해당 시 불가능):
            - 새로운 외부 유료 API 연동 필요
            - DB 테이블 신규 생성 또는 스키마 변경
            - 아키텍처 수준 변경
            - 난이도 높음

            3개 작성 후 마지막에 우선순위 1~3순위와 이유를 간단히 적어주세요.
            """.formatted(trendingContext);
    }

    private List<Map<String, String>> parseProposals(String text) {
        List<Map<String, String>> proposals = new ArrayList<>();
        String[] blocks = text.split("---");
        for (String block : blocks) {
            if (block.isBlank()) continue;
            Map<String, String> proposal = new LinkedHashMap<>();
            for (String line : block.split("\n")) {
                if (line.contains(":")) {
                    String[] parts = line.split(":", 2);
                    proposal.put(parts[0].trim(), parts[1].trim());
                }
            }
            if (proposal.containsKey("기능명")) {
                proposals.add(proposal);
            }
            if (proposals.size() == 3) break;
        }
        return proposals.isEmpty() ? fallbackProposals() : proposals;
    }

    private List<Map<String, String>> fallbackProposals() {
        return List.of(
            Map.of("기능명", "자동 자막 번역", "참고서비스", "CapCut",
                   "구현방법", "생성된 SRT를 Gemini로 다국어 번역", "난이도", "낮음", "예상시간", "2시간",
                   "자동개발", "가능", "자동개발불가이유", ""),
            Map.of("기능명", "배경음악 자동 추천", "참고서비스", "Vrew",
                   "구현방법", "영상 분위기 분석 후 BGM 카테고리 매칭", "난이도", "중간", "예상시간", "4시간",
                   "자동개발", "불가능", "자동개발불가이유", "BGM 라이브러리 외부 API 필요"),
            Map.of("기능명", "영상 A/B 테스트", "참고서비스", "InVideo",
                   "구현방법", "같은 대본으로 2개 썸네일/제목 변형 생성", "난이도", "중간", "예상시간", "3시간",
                   "자동개발", "가능", "자동개발불가이유", "")
        );
    }

    private void sendToSlack(List<Map<String, String>> proposals) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("오늘의 기능 제안 — ").append(LocalDate.now()).append("\n\n");
            int i = 1;
            for (Map<String, String> p : proposals) {
                boolean canAuto = "가능".equals(p.getOrDefault("자동개발", "불가능"));
                String badge = canAuto ? "🤖 CCR 자동 개발 가능" : "👤 수동 개발 필요";
                sb.append("*").append(i++).append(". ").append(p.getOrDefault("기능명", "")).append("*  ").append(badge).append("\n");
                sb.append("• 참고: ").append(p.getOrDefault("참고서비스", "")).append("\n");
                sb.append("• 구현: ").append(p.getOrDefault("구현방법", "")).append("\n");
                sb.append("• 난이도: ").append(p.getOrDefault("난이도", "")).append(" / ")
                  .append(p.getOrDefault("예상시간", ""));
                if (!canAuto) {
                    String reason = p.getOrDefault("자동개발불가이유", "");
                    if (!reason.isBlank()) sb.append("  ⚠️ ").append(reason);
                }
                sb.append("\n\n");
            }
            sb.append("스레드에 *'N번 개발해줘'* 댓글을 달면 CCR이 자동으로 개발합니다. (🤖 표시 기능만)");

            Map<String, String> body = Map.of("text", sb.toString());
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            restTemplate.postForEntity(slackIdeasWebhook, new HttpEntity<>(body, headers), String.class);
            log.info("[FeatureProposal] Slack 전송 완료");
        } catch (Exception e) {
            log.error("[FeatureProposal] Slack 전송 실패: {}", e.getMessage());
        }
    }

    private void recordToDb(List<Map<String, String>> proposals) {
        int priority = 1;
        for (Map<String, String> p : proposals) {
            try {
                boolean autoOk = "가능".equals(p.getOrDefault("자동개발", "불가능"));
                jdbcTemplate.update(
                    "INSERT INTO sm_feature_roadmap " +
                    "(proposal_date, feature_name, reference_service, implementation_desc, " +
                    " difficulty, priority, auto_developable, status) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, '검토중')",
                    LocalDate.now(),
                    p.getOrDefault("기능명", ""),
                    p.getOrDefault("참고서비스", ""),
                    p.getOrDefault("구현방법", ""),
                    p.getOrDefault("난이도", ""),
                    priority++,
                    autoOk);
                log.info("[FeatureProposal] DB 저장 완료: {}", p.get("기능명"));
            } catch (Exception e) {
                log.error("[FeatureProposal] DB 저장 실패: {}", e.getMessage());
            }
        }
    }
}
