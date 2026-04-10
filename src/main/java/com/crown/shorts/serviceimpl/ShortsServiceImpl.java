package com.crown.shorts.serviceimpl;

import com.crown.billing.service.TokenService;
import com.crown.common.service.FcmService;
import com.crown.member.service.MemberService;
import com.crown.shorts.service.UsageLimitService;
import com.crown.shorts.dao.ShortsDao;
import com.crown.shorts.dto.JobDto;
import com.crown.shorts.dto.ProjectDto;
import com.crown.shorts.dto.QuestionDto;
import com.crown.shorts.dto.ScriptHistoryDto;
import com.crown.shorts.dto.SfxItemDto;
import com.crown.shorts.service.ShortsService;
import org.springframework.jdbc.core.JdbcTemplate;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShortsServiceImpl implements ShortsService {

    private final ShortsDao shortsDao;
    private final RestTemplate restTemplate;
    private final FcmService fcmService;
    private final MemberService memberService;
    private final UsageLimitService usageLimitService;
    private final TokenService tokenService;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;

    @Value("${worker.url}")
    private String workerUrl;

    @Value("${worker.secret}")
    private String workerSecret;

    @Value("${app.base-url}")
    private String appBaseUrl;

    // ── 프로젝트 생성 + 워커 호출 ──────────────────────────────────

    @Override
    public List<QuestionDto> getQuestions(String category) {
        return shortsDao.getQuestions(category);
    }

    @Override
    public ProjectDto createAndGenerate(Long memberId, String category, String templateId, Map<String, Object> options) {
        // 사용량 제한 체크 (free: 월 5회)
        String plan = memberService.findById(memberId).getPlan();
        usageLimitService.checkAndRecord(memberId, plan, "GENERATE");

        ProjectDto project = shortsDao.createProject(memberId, category, templateId, options);

        String callbackUrl = appBaseUrl + "/api/shorts/internal/generate-callback/" + project.getProjectId();
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("project_id",   project.getProjectId());
        body.put("callback_url", callbackUrl);
        body.put("template_id",  templateId);
        body.put("category",     category);

        Map<String, Object> mergedOptions = options != null ? new java.util.HashMap<>(options) : new java.util.HashMap<>();

        // PPT 템플릿: DB에서 config 조회 후 options에 포함
        if ("ppt".equals(category) && templateId != null && templateId.matches("\\d+")) {
            try {
                Map<String, Object> tpl = jdbcTemplate.queryForMap(
                    "SELECT config FROM sm_template WHERE id=? AND is_active=1", Integer.parseInt(templateId));
                if (tpl.get("config") != null) {
                    mergedOptions.put("template_config", tpl.get("config").toString());
                }
            } catch (Exception e) {
                log.warn("템플릿 조회 실패 id={}: {}", templateId, e.getMessage());
            }
        }

        if (!mergedOptions.isEmpty()) body.put("options", mergedOptions);

        // category에 따라 워커 엔드포인트 분기
        String workerEndpoint = "ppt".equals(category) ? "/generate/ppt" : "/generate/stock";
        callWorker("POST", workerEndpoint, body);
        shortsDao.updateProjectStatus(project.getProjectId(), "generating");
        project.setStatus("generating");
        return project;
    }

    @Override
    public ProjectDto createBlank(Long memberId, String outputType) {
        Map<String, Object> options = new java.util.HashMap<>();
        options.put("output_type", outputType != null ? outputType : "video");
        return shortsDao.createProject(memberId, "blank", "dark_blue", options);
    }

    @Override
    public byte[] getTtsPreview(String text, String voice, String rate, String emotion) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Worker-Secret", workerSecret);
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("text",  text);
        body.put("voice", voice != null ? voice : "ko-KR-InJoonNeural");
        body.put("rate",  rate  != null ? rate  : "+25%");
        if (emotion != null && !emotion.isEmpty() && !emotion.equals("default")) {
            body.put("emotion", emotion);
        }
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<byte[]> res = restTemplate.exchange(
                workerUrl + "/tts/preview", HttpMethod.POST, entity, byte[].class);
            return res.getBody() != null ? res.getBody() : new byte[0];
        } catch (Exception e) {
            log.error("TTS 미리 듣기 실패: {}", e.getMessage());
            throw new RuntimeException("TTS 미리 듣기 서버 오류: " + e.getMessage(), e);
        }
    }

    // ── 조회 ───────────────────────────────────────────────────────

    @Override
    public List<ProjectDto> getMyProjects(Long memberId) {
        return shortsDao.getProjectsByMemberId(memberId);
    }

    @Override
    public ProjectDto getProject(Long projectId, Long memberId) {
        ProjectDto project = shortsDao.getProjectById(projectId);
        if (project == null) {
            throw new IllegalArgumentException("프로젝트를 찾을 수 없습니다.");
        }
        // 소유자이면 바로 반환
        if (project.getMemberId().equals(memberId)) {
            return project;
        }
        // 팀 멤버 접근 체크
        String teamRole = shortsDao.getTeamRole(projectId, memberId);
        if (teamRole != null) {
            return project;
        }
        throw new IllegalArgumentException("프로젝트를 찾을 수 없습니다.");
    }

    @Override
    public String getProjectAccessRole(Long projectId, Long memberId) {
        ProjectDto project = shortsDao.getProjectById(projectId);
        if (project == null) return "none";
        if (project.getMemberId().equals(memberId)) return "owner";
        String teamRole = shortsDao.getTeamRole(projectId, memberId);
        return teamRole != null ? teamRole : "none";
    }

    // ── 대본 수정 ──────────────────────────────────────────────────

    @Override
    public void updateScript(Long projectId, Long memberId, Map<String, String> script, String note) {
        String role = getProjectAccessRole(projectId, memberId);
        if ("none".equals(role) || "viewer".equals(role)) {
            throw new IllegalArgumentException("수정 권한이 없습니다.");
        }
        try {
            // 기존 대본을 히스토리에 저장 (롤백 가능) — 테이블 없으면 warn만
            try {
                ProjectDto current = shortsDao.getProjectById(projectId);
                if (current.getScript() != null && !current.getScript().isEmpty()) {
                    String historyNote = (note != null && !note.isEmpty()) ? note : "자동 저장";
                    shortsDao.saveScriptHistory(projectId, memberId, current.getScript(), historyNote);
                }
            } catch (Exception histEx) {
                log.warn("[updateScript] 히스토리 저장 실패 (계속 진행): {}", histEx.getMessage());
            }
            String scriptJson = objectMapper.writeValueAsString(script);
            shortsDao.updateProjectScript(projectId, scriptJson);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("대본 직렬화 실패", e);
        }
    }

    // ── HTML 수정 ──────────────────────────────────────────────────

    @Override
    public void updateHtml(Long projectId, Long memberId, String html) {
        getProject(projectId, memberId); // 소유권 확인
        Map<String, Object> body = Map.of(
                "project_id",   projectId,
                "html_content", html
        );
        callWorker("POST", "/html/save", body);
    }

    // ── 제목 변경 ──────────────────────────────────────────────

    @Override
    public void updateTitle(Long projectId, Long memberId, String title) {
        getProject(projectId, memberId);
        shortsDao.updateProjectTitle(projectId, title);
    }

    // ── 프로젝트 삭제 ──────────────────────────────────────────

    // ── 프로젝트 복제 ──────────────────────────────────────────

    @Override
    public ProjectDto duplicateProject(Long projectId, Long memberId) {
        getProject(projectId, memberId); // 소유권 확인
        return shortsDao.duplicateProject(projectId, memberId);
    }

    @Override
    public void deleteProject(Long projectId, Long memberId) {
        getProject(projectId, memberId);
        try {
            callWorker("DELETE", "/storage/project/" + projectId, Map.of());
        } catch (Exception e) {
            log.warn("[project {}] S3 파일 삭제 실패 (DB 삭제 계속 진행): {}", projectId, e.getMessage());
        }
        shortsDao.deleteProject(projectId);
    }

    // ── 영상 생성 ──────────────────────────────────────────────────

    @Override
    public JobDto startRender(Long projectId, Long memberId, Map<String, Object> renderOptions) {
        ProjectDto project = getProject(projectId, memberId);

        // 사용량 제한 체크 (free: 월 5회)
        String plan = memberService.findById(memberId).getPlan();
        usageLimitService.checkAndRecord(memberId, plan, "RENDER");

        // 토큰 차감 (sm_feature_cost 기반)
        tokenService.useTokensForFeature(memberId, "render", projectId);

        JobDto job = shortsDao.createJob(projectId);
        shortsDao.updateProjectStatus(projectId, "generating");

        String callbackUrl = appBaseUrl + "/api/shorts/internal/render-callback/" + job.getJobId()
                + "?projectId=" + projectId;

        String htmlUrl = project.getHtmlUrl() != null ? project.getHtmlUrl() : "";

        Map<String, Object> body = new java.util.HashMap<>();
        body.put("project_id",   projectId);
        body.put("job_id",       job.getJobId());
        body.put("html_url",     htmlUrl);
        body.put("script",       project.getScript());
        body.put("callback_url", callbackUrl);
        Map<String, Object> mergedOptions = new java.util.HashMap<>();
        if (renderOptions != null) mergedOptions.putAll(renderOptions);
        mergedOptions.put("plan", plan);
        body.put("render_options", mergedOptions);
        callWorker("POST", "/render", body);
        shortsDao.updateJobStarted(job.getJobId());
        return job;
    }

    @Override
    public void cancelRender(Long projectId, Long memberId, Long jobId) {
        ProjectDto project = getProject(projectId, memberId);
        JobDto job = shortsDao.getJobById(jobId);

        if (job == null || !job.getProjectId().equals(projectId)) {
            throw new RuntimeException("작업을 찾을 수 없습니다");
        }

        // 이미 완료되었으면 취소 불가
        if ("done".equals(job.getStatus()) || "error".equals(job.getStatus())) {
            throw new RuntimeException("이미 완료된 작업은 취소할 수 없습니다");
        }

        // 워커에 취소 요청
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("job_id", jobId);
        callWorker("POST", "/cancel", body);

        // DB 상태 업데이트
        shortsDao.updateJobFinished(jobId, "cancelled", null);
        shortsDao.updateProjectStatus(projectId, "draft");
    }

    @Override
    public JobDto getJobStatus(Long jobId) {
        return shortsDao.getJobById(jobId);
    }

    // ── 워커 콜백 ──────────────────────────────────────────────────

    @Override
    public void onGenerateDone(Long projectId, String htmlUrl, Map<String, String> script, String title, String thumbnailUrl) {
        try {
            String scriptJson = objectMapper.writeValueAsString(script);
            shortsDao.updateProjectGenerated(projectId, htmlUrl, scriptJson, title, "done");
            if (thumbnailUrl != null && !thumbnailUrl.isBlank()) {
                try {
                    shortsDao.updateProjectThumbnail(projectId, thumbnailUrl);
                } catch (Exception thumbEx) {
                    log.warn("[onGenerateDone] thumbnail_url 업데이트 실패 (컬럼 미생성?): {}", thumbEx.getMessage());
                }
            }
            // FCM 푸시 알림
            sendFcmToProjectOwner(projectId, "슬라이드 생성 완료", "'" + title + "' 슬라이드가 준비되었습니다.");
        } catch (JsonProcessingException e) {
            log.error("대본 직렬화 실패", e);
            shortsDao.updateProjectStatus(projectId, "error");
        }
    }

    @Override
    public void onGenerateError(Long projectId, String errorMessage) {
        log.error("[project {}] 생성 실패: {}", projectId, errorMessage);
        shortsDao.updateProjectStatus(projectId, "error");
        sendFcmToProjectOwner(projectId, "생성 실패", "슬라이드 생성 중 오류가 발생했습니다.");
    }

    /** 공통 토큰 환불 유틸 */
    private void refundFeatureTokens(Long memberId, String featureKey, Long projectId, String reason, int fallbackCost) {
        try {
            Map<String, Object> fc = tokenService.getFeatureCost(featureKey);
            int refundAmount = fc != null ? ((Number) fc.get("tokenCost")).intValue() : fallbackCost;
            tokenService.refundTokens(memberId, refundAmount, reason, projectId);
        } catch (Exception re) {
            log.warn("토큰 환불 실패 [{}]: memberId={}, error={}", featureKey, memberId, re.getMessage());
        }
    }

    @Override
    public void onRenderDone(Long jobId, Long projectId, String videoUrl, String thumbnailUrl) {
        shortsDao.updateProjectVideo(projectId, videoUrl, "done");
        if (thumbnailUrl != null && !thumbnailUrl.isBlank()) {
            try {
                shortsDao.updateProjectThumbnail(projectId, thumbnailUrl);
            } catch (Exception e) {
                log.warn("[onRenderDone] thumbnail_url 업데이트 실패 (무시): {}", e.getMessage());
            }
        }
        shortsDao.updateJobFinished(jobId, "done", null);
        // FCM 푸시 알림
        sendFcmToProjectOwner(projectId, "영상 생성 완료 🎬", "영상이 완성되었습니다. 지금 확인해보세요!");
    }

    @Override
    public void onRenderError(Long jobId, Long projectId, String errorMessage) {
        log.error("[job {}] 렌더링 실패: {}", jobId, errorMessage);
        shortsDao.updateProjectStatus(projectId, "error");
        shortsDao.updateJobFinished(jobId, "error", errorMessage);

        // 토큰 환불
        try {
            ProjectDto project = shortsDao.getProjectById(projectId);
            if (project != null) {
                refundFeatureTokens(project.getMemberId(), "render", projectId, "영상 생성 실패 환불", 10);
            }
        } catch (Exception e) {
            log.warn("[onRenderError] 토큰 환불 실패 (무시): {}", e.getMessage());
        }

        sendFcmToProjectOwner(projectId, "영상 생성 실패", "영상 생성 중 오류가 발생했습니다.");
    }

    private void sendFcmToProjectOwner(Long projectId, String title, String body) {
        try {
            ProjectDto project = shortsDao.getProjectById(projectId);
            if (project == null) return;
            String fcmToken = memberService.getFcmToken(project.getMemberId());
            fcmService.send(fcmToken, title, body, String.valueOf(projectId));
        } catch (Exception e) {
            log.warn("[FCM] 프로젝트 {} 알림 실패: {}", projectId, e.getMessage());
        }
    }

    // ── AI 재작성 ───────────────────────────────────────────────────

    @Override
    public String rewriteScript(Long projectId, Long memberId, String text, String style, String instruction) {
        tokenService.useTokensForFeature(memberId, "ai_rewrite", projectId);
        getProject(projectId, memberId);
        try {
            Map<String, Object> body = new java.util.HashMap<>();
            body.put("text",        text);
            body.put("style",       style != null ? style : "news");
            body.put("instruction", instruction != null ? instruction : "");
            Map<String, Object> res = callWorkerJson("POST", "/ai/rewrite", body);
            Object data = res.get("data");
            return data != null ? data.toString() : "";
        } catch (Exception e) {
            refundFeatureTokens(memberId, "ai_rewrite", projectId, "AI 재작성 실패 환불", 3);
            throw e;
        }
    }

    @Override
    public String translateScript(Long projectId, Long memberId, String text, String targetLanguage) {
        tokenService.useTokensForFeature(memberId, "translate", projectId);
        getProject(projectId, memberId);
        try {
            Map<String, Object> body = new java.util.HashMap<>();
            body.put("text",            text);
            body.put("target_language", targetLanguage != null ? targetLanguage : "en");
            Map<String, Object> res = callWorkerJson("POST", "/ai/translate", body);
            Object data = res.get("data");
            return data != null ? data.toString() : "";
        } catch (Exception e) {
            refundFeatureTokens(memberId, "translate", projectId, "번역 실패 환불", 3);
            throw e;
        }
    }

    // ── 트렌딩 토픽 ────────────────────────────────────────────────────

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getTrendingTopics(String category, int limit) {
        String path = "/trending/topics?category=" + (category != null ? category : "stock")
                + "&limit=" + (limit > 0 ? limit : 10);
        try {
            Map<String, Object> res = callWorkerJson("GET", path, null);
            Object data = res.get("data");
            if (data instanceof List) return (List<Map<String, Object>>) data;
        } catch (Exception e) {
            log.warn("트렌딩 토픽 조회 실패: {}", e.getMessage());
        }
        return new java.util.ArrayList<>();
    }

    // ── 효과음 라이브러리 ──────────────────────────────────────────────

    private static final List<SfxItemDto> SFX_LIBRARY = List.of(
        new SfxItemDto(1,  "또잉! 등장음",                      0.21, List.of("시선집중","카툰","게임"),         null),
        new SfxItemDto(2,  "북소리 (두둥)",                     1.70, List.of("소리효과","장면전환","트랜지션"),  null),
        new SfxItemDto(3,  "다음으로 넘어가는 맑은 소리...",    2.06, List.of("소리효과","시선집중","장면전환"),  null),
        new SfxItemDto(4,  "쫄래쫄래 걸어가는 소리(뚝뚝)",     2.11, List.of("소리효과","게임","전자음"),        null),
        new SfxItemDto(5,  "집중용 띵 2",                       3.63, List.of("소리효과","장면전환","트랜지션"),  null),
        new SfxItemDto(6,  "깨달음을 얻었을 때(뽀봉)",          1.80, List.of("소리효과","카툰","게임"),          null),
        new SfxItemDto(7,  "가볍게 휘두르는 소리",              0.63, List.of("소리효과","장면전환","트랜지션"),  null),
        new SfxItemDto(8,  "아이들 환호 소리(yeah)",            2.56, List.of("소리효과","분위기","긍정"),        null),
        new SfxItemDto(9,  "긍정적 마법 사용",                  1.45, List.of("소리효과","카툰","긍정"),          null),
        new SfxItemDto(10, "카운트다운 비프",                   3.00, List.of("전자음","시선집중","긴장"),         null),
        new SfxItemDto(11, "짧은 성공음",                       0.80, List.of("긍정","게임","알림"),              null),
        new SfxItemDto(12, "실패·오류음",                       0.90, List.of("소리효과","게임","경고"),          null),
        new SfxItemDto(13, "알림 팝업",                         0.40, List.of("알림","전자음","시선집중"),         null),
        new SfxItemDto(14, "드라마틱 타악기",                   2.20, List.of("장면전환","긴장","드라마틱"),       null),
        new SfxItemDto(15, "경쾌한 클릭",                       0.15, List.of("전자음","UI","클릭"),              null),
        new SfxItemDto(16, "부드러운 전환음",                   1.10, List.of("장면전환","분위기","트랜지션"),     null),
        new SfxItemDto(17, "두근두근 심장 소리",                2.40, List.of("긴장","드라마틱","분위기"),         null),
        new SfxItemDto(18, "번개 효과",                         0.55, List.of("소리효과","게임","전자음"),         null),
        new SfxItemDto(19, "봄바람 휘파람",                     3.10, List.of("분위기","자연","배경"),             null),
        new SfxItemDto(20, "군중 박수",                         4.00, List.of("긍정","분위기","배경"),             null),
        new SfxItemDto(21, "타이핑 소리",                       1.20, List.of("전자음","UI","분위기"),             null),
        new SfxItemDto(22, "줌인 효과음",                       0.35, List.of("시선집중","전자음","게임"),         null),
        new SfxItemDto(23, "로켓 발사",                         1.90, List.of("소리효과","긍정","긴장"),           null),
        new SfxItemDto(24, "동전 획득",                         0.70, List.of("게임","긍정","카툰"),               null),
        new SfxItemDto(25, "경고 사이렌",                       2.80, List.of("경고","긴장","드라마틱"),           null),
        new SfxItemDto(26, "새 지저귀는 소리",                  3.50, List.of("자연","분위기","배경"),             null),
        new SfxItemDto(27, "전화 수신음",                       2.00, List.of("알림","소리효과","UI"),             null),
        new SfxItemDto(28, "문 삐걱 소리",                      1.30, List.of("소리효과","분위기","공포"),         null),
        new SfxItemDto(29, "폭발음 (작은)",                     1.00, List.of("소리효과","게임","드라마틱"),        null),
        new SfxItemDto(30, "레벨업 효과음",                     1.50, List.of("게임","긍정","전자음"),             null)
    );

    @Override
    public List<SfxItemDto> getSfxLibrary(String q, String tab) {
        // DB 조회 (sm_sfx 테이블). 비어있으면 하드코딩 fallback
        try {
            String sql = "SELECT id, name, tags, duration, s3_url FROM sm_sfx";
            Object[] params;
            if (q != null && !q.isBlank()) {
                sql += " WHERE name LIKE ? OR tags LIKE ?";
                params = new Object[]{"%" + q + "%", "%" + q + "%"};
            } else {
                params = new Object[]{};
            }
            sql += " ORDER BY id DESC";
            List<SfxItemDto> rows = jdbcTemplate.query(sql, params, (rs, i) -> {
                String tagsStr = rs.getString("tags");
                List<String> tags = new java.util.ArrayList<>();
                if (tagsStr != null && !tagsStr.isBlank()) {
                    for (String t : tagsStr.replaceAll("[\\[\\]\"]", "").split(",")) {
                        String trimmed = t.trim();
                        if (!trimmed.isEmpty()) tags.add(trimmed);
                    }
                }
                return new SfxItemDto(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getDouble("duration"),
                    tags,
                    rs.getString("s3_url")
                );
            });
            if (!rows.isEmpty()) return rows;
        } catch (Exception e) {
            log.warn("sm_sfx DB 조회 실패, fallback 사용: {}", e.getMessage());
        }
        // fallback: 하드코딩 데이터
        java.util.stream.Stream<SfxItemDto> stream = SFX_LIBRARY.stream();
        if (q != null && !q.isBlank()) {
            String keyword = q.trim().toLowerCase();
            stream = stream.filter(item ->
                item.getName().toLowerCase().contains(keyword) ||
                item.getTags().stream().anyMatch(tag -> tag.toLowerCase().contains(keyword))
            );
        }
        return stream.collect(java.util.stream.Collectors.toList());
    }

    @Override
    public List<SfxItemDto> getBgmLibrary(String q) {
        try {
            String sql = "SELECT id, name, tags, duration, s3_url FROM sm_bgm";
            Object[] params;
            if (q != null && !q.isBlank()) {
                sql += " WHERE name LIKE ? OR tags LIKE ?";
                params = new Object[]{"%" + q + "%", "%" + q + "%"};
            } else {
                params = new Object[]{};
            }
            sql += " ORDER BY id DESC";
            List<SfxItemDto> rows = jdbcTemplate.query(sql, params, (rs, i) -> {
                String tagsStr = rs.getString("tags");
                List<String> tags = new java.util.ArrayList<>();
                if (tagsStr != null && !tagsStr.isBlank()) {
                    for (String t : tagsStr.replaceAll("[\\[\\]\"]", "").split(",")) {
                        String trimmed = t.trim();
                        if (!trimmed.isEmpty()) tags.add(trimmed);
                    }
                }
                return new SfxItemDto(rs.getInt("id"), rs.getString("name"),
                        rs.getDouble("duration"), tags, rs.getString("s3_url"));
            });
            return rows;
        } catch (Exception e) {
            log.warn("sm_bgm DB 조회 실패: {}", e.getMessage());
            return java.util.Collections.emptyList();
        }
    }

    // ── 대본 히스토리 ──────────────────────────────────────────────────

    @Override
    public ScriptHistoryDto saveScriptHistory(Long projectId, Long memberId, Map<String, String> script, String note) {
        getProject(projectId, memberId);
        return shortsDao.saveScriptHistory(projectId, memberId, script, note);
    }

    @Override
    public List<ScriptHistoryDto> getScriptHistory(Long projectId, Long memberId) {
        getProject(projectId, memberId);
        return shortsDao.getScriptHistory(projectId);
    }

    @Override
    public void restoreScriptHistory(Long projectId, Long memberId, Long historyId) {
        getProject(projectId, memberId);
        ScriptHistoryDto history = shortsDao.getScriptHistoryById(historyId);
        if (history == null || !history.getProjectId().equals(projectId)) {
            throw new IllegalArgumentException("히스토리를 찾을 수 없습니다.");
        }
        try {
            String scriptJson = objectMapper.writeValueAsString(history.getScript());
            shortsDao.updateProjectScript(projectId, scriptJson);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("대본 직렬화 실패", e);
        }
    }

    // ── AI 강화 — 해시태그 / SEO / 품질 분석 ─────────────────────────

    @Override
    @SuppressWarnings("unchecked")
    public List<String> generateHashtags(Long projectId, Long memberId, String title, String script, int count) {
        tokenService.useTokensForFeature(memberId, "hashtag", projectId);
        getProject(projectId, memberId);
        try {
            Map<String, Object> body = new java.util.HashMap<>();
            body.put("title",  title  != null ? title  : "");
            body.put("script", script != null ? script : "");
            body.put("count",  count > 0 ? count : 15);
            Map<String, Object> res = callWorkerJson("POST", "/ai/hashtags", body);
            Object data = res.get("data");
            if (data instanceof List) return (List<String>) data;
            return new java.util.ArrayList<>();
        } catch (Exception e) {
            refundFeatureTokens(memberId, "hashtag", projectId, "해시태그 생성 실패 환불", 2);
            throw e;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> generateSeo(Long projectId, Long memberId, String title, String script) {
        tokenService.useTokensForFeature(memberId, "seo", projectId);
        getProject(projectId, memberId);
        try {
            Map<String, Object> body = new java.util.HashMap<>();
            body.put("title",  title  != null ? title  : "");
            body.put("script", script != null ? script : "");
            Map<String, Object> res = callWorkerJson("POST", "/ai/seo", body);
            Object data = res.get("data");
            if (data instanceof Map) return (Map<String, Object>) data;
            return new java.util.HashMap<>();
        } catch (Exception e) {
            refundFeatureTokens(memberId, "seo", projectId, "SEO 생성 실패 환불", 3);
            throw e;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> analyzeQuality(Long projectId, Long memberId, String script) {
        tokenService.useTokensForFeature(memberId, "quality_analysis", projectId);
        getProject(projectId, memberId);
        try {
            Map<String, Object> body = new java.util.HashMap<>();
            body.put("script", script != null ? script : "");
            Map<String, Object> res = callWorkerJson("POST", "/ai/quality", body);
            Object data = res.get("data");
            if (data instanceof Map) return (Map<String, Object>) data;
            return new java.util.HashMap<>();
        } catch (Exception e) {
            refundFeatureTokens(memberId, "quality_analysis", projectId, "품질 분석 실패 환불", 3);
            throw e;
        }
    }

    // ── 자막 생성 ───────────────────────────────────────────────────

    @Override
    public Map<String, Object> generateSubtitleFromScript(Long projectId, Long memberId, Map<String, String> script, String ttsRate) {
        tokenService.useTokensForFeature(memberId, "subtitle_script", projectId);
        getProject(projectId, memberId);
        try {
            Map<String, Object> body = new java.util.HashMap<>();
            body.put("project_id", projectId);
            body.put("script",     script);
            body.put("tts_rate",   ttsRate != null ? ttsRate : "+25%");
            Map<String, Object> res = callWorkerJson("POST", "/subtitle/from-script", body);
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) res.get("data");
            return data != null ? data : new java.util.HashMap<>();
        } catch (Exception e) {
            refundFeatureTokens(memberId, "subtitle_script", projectId, "자막 생성 실패 환불", 3);
            throw e;
        }
    }

    @Override
    public Map<String, Object> generateSubtitleFromVideo(Long projectId, Long memberId, String videoUrl, String language) {
        tokenService.useTokensForFeature(memberId, "subtitle_video", projectId);
        getProject(projectId, memberId);
        try {
            Map<String, Object> body = new java.util.HashMap<>();
            body.put("project_id", projectId);
            body.put("video_url",  videoUrl);
            body.put("language",   language != null ? language : "ko");
            Map<String, Object> res = callWorkerJson("POST", "/subtitle/from-video", body);
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) res.get("data");
            return data != null ? data : new java.util.HashMap<>();
        } catch (Exception e) {
            refundFeatureTokens(memberId, "subtitle_video", projectId, "영상 자막 생성 실패 환불", 5);
            throw e;
        }
    }

    public Map<String, Object> translateSubtitle(Long projectId, Long memberId, String srt, String targetLanguage) {
        tokenService.useTokensForFeature(memberId, "subtitle_translate", projectId);
        getProject(projectId, memberId);
        try {
            Map<String, Object> body = new java.util.HashMap<>();
            body.put("project_id",       projectId);
            body.put("srt",              srt);
            body.put("target_language",  targetLanguage != null ? targetLanguage : "en");
            Map<String, Object> res = callWorkerJson("POST", "/subtitle/translate", body);
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) res.get("data");
            return data != null ? data : new java.util.HashMap<>();
        } catch (Exception e) {
            refundFeatureTokens(memberId, "subtitle_translate", projectId, "자막 번역 실패 환불", 3);
            throw e;
        }
    }

    // ── 목소리 복제 ─────────────────────────────────────────────────

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> cloneVoice(String name, String description, byte[] sampleBytes, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("X-Worker-Secret", workerSecret);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("name", name);
        body.add("description", description != null ? description : "");
        body.add("sample", new ByteArrayResource(sampleBytes) {
            @Override public String getFilename() { return filename != null ? filename : "sample.mp3"; }
        });

        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(workerUrl + "/voice/clone", entity, Map.class);
            Map<String, Object> res = response.getBody();
            return res != null ? res : new java.util.HashMap<>();
        } catch (Exception e) {
            log.error("목소리 복제 실패: {}", e.getMessage());
            throw new RuntimeException("목소리 복제 서버에 연결할 수 없습니다.", e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listVoices() {
        List<Map<String, Object>> result = new java.util.ArrayList<>();

        // 1) edge-tts 기본 목소리 (Python Worker /tts/voices)
        try {
            Map<String, Object> ttsRes = callWorkerJson("GET", "/tts/voices", null);
            Object ttsData = ttsRes.get("data");
            if (ttsData instanceof Map) {
                Map<String, List<Map<String, Object>>> langMap = (Map<String, List<Map<String, Object>>>) ttsData;
                for (var entry : langMap.entrySet()) {
                    for (Map<String, Object> v : entry.getValue()) {
                        Map<String, Object> item = new java.util.HashMap<>(v);
                        item.put("category", "edge_tts");
                        item.put("lang", entry.getKey());
                        result.add(item);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("edge-tts 목소리 목록 조회 실패: {}", e.getMessage());
        }

        // 2) ElevenLabs 복제 목소리 (Python Worker /voice/list)
        try {
            Map<String, Object> res = callWorkerJson("GET", "/voice/list", null);
            Object data = res.get("data");
            if (data instanceof List) {
                for (Map<String, Object> v : (List<Map<String, Object>>) data) {
                    Map<String, Object> item = new java.util.HashMap<>(v);
                    item.putIfAbsent("category", "elevenlabs");
                    result.add(item);
                }
            }
        } catch (Exception e) {
            log.warn("ElevenLabs 목소리 목록 조회 실패: {}", e.getMessage());
        }

        return result;
    }

    @Override
    public void deleteVoice(String voiceId) {
        callWorker("DELETE", "/voice/" + voiceId, new java.util.HashMap<>());
    }

    // ── 클립 미디어 업로드 ──────────────────────────────────────────

    @Override
    @SuppressWarnings("unchecked")
    public String uploadAsset(Long projectId, Long memberId, byte[] fileBytes, String filename, String contentType) {
        getProject(projectId, memberId); // 소유권 확인

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("X-Worker-Secret", workerSecret);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("project_id", String.valueOf(projectId));
        final String finalFilename = filename != null ? filename : "asset";
        body.add("file", new ByteArrayResource(fileBytes) {
            @Override public String getFilename() { return finalFilename; }
        });

        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<Map> res = restTemplate.postForEntity(
                    workerUrl + "/storage/upload", entity, Map.class);
            Map<?, ?> resBody = res.getBody();
            return resBody != null ? (String) resBody.get("url") : null;
        } catch (Exception e) {
            log.error("에셋 업로드 실패: {}", e.getMessage());
            throw new RuntimeException("에셋 업로드 서버 오류: " + e.getMessage(), e);
        }
    }

    // ── PPT 슬라이드 생성 ──────────────────────────────────────────

    @Override
    public void generatePptSlides(Long projectId, Long memberId, Map<String, Object> options) {
        tokenService.useTokensForFeature(memberId, "ppt_generate", projectId);
        ProjectDto project = getProject(projectId, memberId);
        try {
            String callbackUrl = appBaseUrl + "/api/shorts/internal/generate-callback/" + projectId;
            Map<String, Object> body = new java.util.HashMap<>();
            body.put("project_id",   projectId);
            body.put("callback_url", callbackUrl);
            body.put("template_id",  options != null ? options.getOrDefault("templateId", "dark_blue") : "dark_blue");
            if (options != null) body.put("options", options);
            callWorker("POST", "/generate/ppt", body);
            shortsDao.updateProjectStatus(projectId, "generating");
        } catch (Exception e) {
            refundFeatureTokens(memberId, "ppt_generate", projectId, "PPT 생성 실패 환불", 5);
            throw e;
        }
    }

    // ── PDF / PPTX 내보내기 ────────────────────────────────────────

    @Override
    @SuppressWarnings("unchecked")
    public String exportPdf(Long projectId, Long memberId) {
        tokenService.useTokensForFeature(memberId, "export_pdf", projectId);
        ProjectDto project = getProject(projectId, memberId);
        String htmlUrl = project.getHtmlUrl();
        if (htmlUrl == null || htmlUrl.isBlank()) {
            refundFeatureTokens(memberId, "export_pdf", projectId, "PDF 내보내기 실패 환불 (HTML 없음)", 3);
            throw new RuntimeException("HTML이 없습니다. 먼저 슬라이드를 저장해주세요.");
        }
        try {
            Map<String, Object> body = Map.of("project_id", projectId, "html_url", htmlUrl);
            Map<String, Object> res = callWorkerJson("POST", "/export/pdf", body);
            Object url = res.get("url");
            if (url == null) throw new RuntimeException("PDF 내보내기 실패");
            return url.toString();
        } catch (Exception e) {
            refundFeatureTokens(memberId, "export_pdf", projectId, "PDF 내보내기 실패 환불", 3);
            throw e;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public String exportPptx(Long projectId, Long memberId) {
        tokenService.useTokensForFeature(memberId, "export_pptx", projectId);
        ProjectDto project = getProject(projectId, memberId);
        String htmlUrl = project.getHtmlUrl();
        if (htmlUrl == null || htmlUrl.isBlank()) {
            refundFeatureTokens(memberId, "export_pptx", projectId, "PPTX 내보내기 실패 환불 (HTML 없음)", 3);
            throw new RuntimeException("HTML이 없습니다. 먼저 슬라이드를 저장해주세요.");
        }
        try {
            Map<String, Object> body = Map.of("project_id", projectId, "html_url", htmlUrl);
            Map<String, Object> res = callWorkerJson("POST", "/export/pptx", body);
            Object url = res.get("url");
            if (url == null) throw new RuntimeException("PPTX 내보내기 실패");
            return url.toString();
        } catch (Exception e) {
            refundFeatureTokens(memberId, "export_pptx", projectId, "PPTX 내보내기 실패 환불", 3);
            throw e;
        }
    }

    // ── 배치 스케줄 ──────────────────────────────────────────────────

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getSchedules(Long memberId) {
        Map<String, Object> result = callWorkerJson("GET", "/schedule?member_id=" + memberId, null);
        Object data = result.get("data");
        return data instanceof List ? (List<Map<String, Object>>) data : List.of();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> createSchedule(Long memberId, Map<String, Object> body) {
        body = new java.util.HashMap<>(body);
        body.put("member_id", memberId);
        Map<String, Object> result = callWorkerJson("POST", "/schedule", body);
        Object data = result.get("data");
        return data instanceof Map ? (Map<String, Object>) data : Map.of();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getSchedule(Long scheduleId) {
        Map<String, Object> result = callWorkerJson("GET", "/schedule/" + scheduleId, null);
        Object data = result.get("data");
        return data instanceof Map ? (Map<String, Object>) data : Map.of();
    }

    @Override
    public Map<String, Object> updateSchedule(Long scheduleId, Map<String, Object> body) {
        callWorkerJson("PUT", "/schedule/" + scheduleId, body);
        return Map.of("updated", true);
    }

    @Override
    public void deleteSchedule(Long scheduleId) {
        callWorkerJson("DELETE", "/schedule/" + scheduleId, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> toggleSchedule(Long scheduleId) {
        Map<String, Object> result = callWorkerJson("PATCH", "/schedule/" + scheduleId + "/toggle", null);
        Object data = result.get("data");
        return data instanceof Map ? (Map<String, Object>) data : Map.of();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getScheduleLogs(Long scheduleId, int limit) {
        Map<String, Object> result = callWorkerJson("GET", "/schedule/" + scheduleId + "/logs?limit=" + limit, null);
        Object data = result.get("data");
        return data instanceof List ? (List<Map<String, Object>>) data : List.of();
    }

    // ── 영상 내보내기 (해상도 변환) ───────────────────────────────────

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> exportVideo(Long projectId, Long memberId, String resolution) {
        ProjectDto project = getProject(projectId, memberId);
        if (project.getVideoUrl() == null || project.getVideoUrl().isEmpty()) {
            throw new RuntimeException("영상이 아직 생성되지 않았습니다.");
        }

        // Pro 이상 해상도 체크
        String plan = memberService.findById(memberId).getPlan();
        java.util.Set<String> proResolutions = java.util.Set.of("1440x2560", "2160x3840", "1080x1920:hq");
        if (proResolutions.contains(resolution) && ("free".equals(plan) || plan == null)) {
            throw new RuntimeException("Pro 플랜 이상에서 사용할 수 있는 해상도입니다.");
        }

        Map<String, Object> body = new java.util.HashMap<>();
        body.put("project_id", projectId);
        body.put("video_url", project.getVideoUrl());
        body.put("resolution", resolution);
        body.put("plan", plan != null ? plan : "free");

        Map<String, Object> result = callWorkerJson("POST", "/export/video", body);
        return result;
    }

    // ── 코멘트/피드백 ─────────────────────────────────────────────────

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getComments(Long projectId) {
        Map<String, Object> result = callWorkerJson("GET", "/comment?project_id=" + projectId, null);
        Object data = result.get("data");
        return data instanceof List ? (List<Map<String, Object>>) data : List.of();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> createComment(Long memberId, Map<String, Object> body) {
        body = new java.util.HashMap<>(body);
        body.put("member_id", memberId);
        Map<String, Object> result = callWorkerJson("POST", "/comment", body);
        Object data = result.get("data");
        return data instanceof Map ? (Map<String, Object>) data : Map.of();
    }

    @Override
    public Map<String, Object> updateComment(Long commentId, String content) {
        callWorkerJson("PUT", "/comment/" + commentId, Map.of("content", content));
        return Map.of("updated", true);
    }

    @Override
    public void deleteComment(Long commentId) {
        callWorkerJson("DELETE", "/comment/" + commentId, null);
    }

    // ── 팀 협업 ──────────────────────────────────────────────────────

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getTeams(Long memberId) {
        Map<String, Object> result = callWorkerJson("GET", "/team?member_id=" + memberId, null);
        Object data = result.get("data");
        return data instanceof List ? (List<Map<String, Object>>) data : List.of();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> createTeam(Long memberId, Map<String, Object> body) {
        body = new java.util.HashMap<>(body);
        body.put("owner_id", memberId);
        Map<String, Object> result = callWorkerJson("POST", "/team", body);
        Object data = result.get("data");
        return data instanceof Map ? (Map<String, Object>) data : Map.of();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getTeam(Long teamId) {
        Map<String, Object> result = callWorkerJson("GET", "/team/" + teamId, null);
        Object data = result.get("data");
        return data instanceof Map ? (Map<String, Object>) data : Map.of();
    }

    @Override
    public void deleteTeam(Long teamId) {
        callWorkerJson("DELETE", "/team/" + teamId, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> inviteTeamMember(Long teamId, Map<String, Object> body) {
        Map<String, Object> result = callWorkerJson("POST", "/team/" + teamId + "/invite", body);
        Object data = result.get("data");
        return data instanceof Map ? (Map<String, Object>) data : Map.of();
    }

    @Override
    public Map<String, Object> updateTeamMemberRole(Long teamId, Long memberId, String role) {
        callWorkerJson("PUT", "/team/" + teamId + "/member/" + memberId + "/role",
                Map.of("role", role));
        return Map.of("updated", true);
    }

    @Override
    public void removeTeamMember(Long teamId, Long memberId) {
        callWorkerJson("DELETE", "/team/" + teamId + "/member/" + memberId, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> acceptTeamInvite(Long teamId, Long memberId) {
        Map<String, Object> result = callWorkerJson("PUT",
                "/team/" + teamId + "/accept?member_id=" + memberId, null);
        Object data = result.get("data");
        return data instanceof Map ? (Map<String, Object>) data : Map.of();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> shareTeamProject(Long teamId, Long sharedBy, Map<String, Object> body) {
        body = new java.util.HashMap<>(body);
        body.put("shared_by", sharedBy);
        Map<String, Object> result = callWorkerJson("POST", "/team/" + teamId + "/project", body);
        Object data = result.get("data");
        return data instanceof Map ? (Map<String, Object>) data : Map.of();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getTeamProjects(Long teamId) {
        Map<String, Object> result = callWorkerJson("GET", "/team/" + teamId + "/project", null);
        Object data = result.get("data");
        return data instanceof List ? (List<Map<String, Object>>) data : List.of();
    }

    @Override
    public void removeTeamProject(Long teamId, Long projectId) {
        callWorkerJson("DELETE", "/team/" + teamId + "/project/" + projectId, null);
    }

    // ── 내부 유틸 ──────────────────────────────────────────────────

    private void callWorker(String method, String path, Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Worker-Secret", workerSecret);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            // secret 파라미터 추가 (기존 쿼리스트링 유무 판단)
            String sep = path.contains("?") ? "&" : "?";
            String url = workerUrl + path + sep + "secret=" + workerSecret;
            restTemplate.exchange(url, HttpMethod.valueOf(method), entity, String.class);
        } catch (Exception e) {
            log.error("Python 워커 호출 실패 [{}{}]: {}", method, path, e.getMessage());
            throw new RuntimeException("영상 생성 서버에 연결할 수 없습니다.", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> callWorkerJson(String method, String path, Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Worker-Secret", workerSecret);
        HttpEntity<Map<String, Object>> entity = body != null
            ? new HttpEntity<>(body, headers)
            : new HttpEntity<>(headers);

        try {
            // secret 파라미터 추가
            String sep = path.contains("?") ? "&" : "?";
            String url = workerUrl + path + sep + "secret=" + workerSecret;
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.valueOf(method), entity, Map.class);
            return response.getBody() != null ? response.getBody() : new java.util.HashMap<>();
        } catch (Exception e) {
            log.error("Python 워커 호출 실패 [{}{}]: {}", method, path, e.getMessage());
            throw new RuntimeException("AI 서버에 연결할 수 없습니다.", e);
        }
    }
}
