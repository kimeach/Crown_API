package com.crown.shorts.restcontroller;

import com.crown.common.dto.ApiResponse;
import com.crown.member.service.MemberService;
import com.crown.shorts.dto.JobDto;
import com.crown.shorts.dto.ProjectDto;
import com.crown.shorts.dto.QuestionDto;
import com.crown.shorts.dto.SfxItemDto;
import com.crown.shorts.service.ProgressService;
import com.crown.shorts.service.ShortsService;
import com.crown.shorts.service.UsageLimitService;
import com.google.firebase.auth.FirebaseToken;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/shorts")
@RequiredArgsConstructor
public class ShortsRestController {

    private final ShortsService shortsService;
    private final MemberService memberService;
    private final ProgressService progressService;
    private final UsageLimitService usageLimitService;

    /** 카테고리별 설문 질문 조회 */
    @GetMapping("/questions")
    public ApiResponse<List<QuestionDto>> getQuestions(
            @RequestParam(defaultValue = "stock") String category) {
        return ApiResponse.ok(shortsService.getQuestions(category));
    }

    /** 무료 효과음 라이브러리 조회 */
    @GetMapping("/sfx")
    public ApiResponse<List<SfxItemDto>> getSfxLibrary(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "all") String tab) {
        return ApiResponse.ok(shortsService.getSfxLibrary(q, tab));
    }

    /** 배경음악 라이브러리 조회 */
    @GetMapping("/bgm")
    public ApiResponse<List<SfxItemDto>> getBgmLibrary(
            @RequestParam(required = false) String q) {
        return ApiResponse.ok(shortsService.getBgmLibrary(q));
    }

    /** 데이터 수집 + 대본 + HTML 생성 요청 */
    @PostMapping("/projects/generate")
    public ApiResponse<ProjectDto> generate(
            @AuthenticationPrincipal FirebaseToken token,
            @RequestBody Map<String, Object> body) {
        Long memberId = memberService.findByGoogleId(token.getUid()).getMemberId();
        String category   = (String) body.getOrDefault("category",    "stock");
        String templateId = (String) body.getOrDefault("template_id", "dark_blue");
        @SuppressWarnings("unchecked")
        Map<String, Object> options = (Map<String, Object>) body.get("options");
        return ApiResponse.ok(shortsService.createAndGenerate(memberId, category, templateId, options));
    }

    /** TTS 미리 듣기 — 오디오 바이트 스트리밍 */
    @PostMapping("/tts-preview")
    public ResponseEntity<byte[]> ttsPreview(
            @AuthenticationPrincipal FirebaseToken token,
            @RequestBody Map<String, Object> body) {
        byte[] audio = shortsService.getTtsPreview(
                (String) body.getOrDefault("text",  ""),
                (String) body.getOrDefault("voice", "ko-KR-InJoonNeural"),
                (String) body.getOrDefault("rate",  "+25%"),
                (String) body.getOrDefault("emotion", null)
        );
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.valueOf("audio/mpeg").toString())
                .body(audio);
    }

    /** 빈 프로젝트 생성 */
    @PostMapping("/projects/blank")
    public ApiResponse<ProjectDto> createBlank(
            @AuthenticationPrincipal FirebaseToken token,
            @RequestBody(required = false) Map<String, Object> body) {
        Long memberId = memberService.findByGoogleId(token.getUid()).getMemberId();
        String outputType = (body != null && body.containsKey("output_type"))
                ? body.get("output_type").toString() : "video";
        return ApiResponse.ok(shortsService.createBlank(memberId, outputType));
    }

    /** 내 프로젝트 목록 */
    @GetMapping("/projects")
    public ApiResponse<List<ProjectDto>> getProjects(@AuthenticationPrincipal FirebaseToken token) {
        Long memberId = memberService.findByGoogleId(token.getUid()).getMemberId();
        return ApiResponse.ok(shortsService.getMyProjects(memberId));
    }

    /** 프로젝트 상세 */
    @GetMapping("/projects/{projectId}")
    public ApiResponse<ProjectDto> getProject(
            @AuthenticationPrincipal FirebaseToken token,
            @PathVariable Long projectId) {
        Long memberId = memberService.findByGoogleId(token.getUid()).getMemberId();
        return ApiResponse.ok(shortsService.getProject(projectId, memberId));
    }

    /** HTML 수정 저장 */
    @PutMapping("/projects/{projectId}/html")
    public ApiResponse<Void> updateHtml(
            @AuthenticationPrincipal FirebaseToken token,
            @PathVariable Long projectId,
            @RequestBody Map<String, String> body) {
        Long memberId = memberService.findByGoogleId(token.getUid()).getMemberId();
        shortsService.updateHtml(projectId, memberId, body.get("html"));
        return ApiResponse.ok(null);
    }

    /** 대본 수정 저장 */
    @PutMapping("/projects/{projectId}/script")
    public ApiResponse<Void> updateScript(
            @AuthenticationPrincipal FirebaseToken token,
            @PathVariable Long projectId,
            @RequestBody Map<String, String> script) {
        Long memberId = memberService.findByGoogleId(token.getUid()).getMemberId();
        shortsService.updateScript(projectId, memberId, script);
        return ApiResponse.ok(null);
    }

    /** 제목 변경 */
    @PatchMapping("/projects/{projectId}/title")
    public ApiResponse<Void> updateTitle(
            @AuthenticationPrincipal FirebaseToken token,
            @PathVariable Long projectId,
            @RequestBody Map<String, String> body) {
        Long memberId = memberService.findByGoogleId(token.getUid()).getMemberId();
        shortsService.updateTitle(projectId, memberId, body.get("title"));
        return ApiResponse.ok(null);
    }

    /** 프로젝트 복제 */
    @PostMapping("/projects/{projectId}/duplicate")
    public ApiResponse<ProjectDto> duplicateProject(
            @AuthenticationPrincipal FirebaseToken token,
            @PathVariable Long projectId) {
        Long memberId = memberService.findByGoogleId(token.getUid()).getMemberId();
        return ApiResponse.ok(shortsService.duplicateProject(projectId, memberId));
    }

    /** 프로젝트 삭제 */
    @DeleteMapping("/projects/{projectId}")
    public ApiResponse<Void> deleteProject(
            @AuthenticationPrincipal FirebaseToken token,
            @PathVariable Long projectId) {
        Long memberId = memberService.findByGoogleId(token.getUid()).getMemberId();
        shortsService.deleteProject(projectId, memberId);
        return ApiResponse.ok(null);
    }

    /** 영상 생성 시작 */
    @PostMapping("/projects/{projectId}/render")
    public ApiResponse<JobDto> render(
            @AuthenticationPrincipal FirebaseToken token,
            @PathVariable Long projectId,
            @RequestBody(required = false) Map<String, Object> body) {
        Long memberId = memberService.findByGoogleId(token.getUid()).getMemberId();
        @SuppressWarnings("unchecked")
        Map<String, Object> renderOptions = (body != null)
                ? (Map<String, Object>) body.get("render_options") : null;
        return ApiResponse.ok(shortsService.startRender(projectId, memberId, renderOptions));
    }

    /** 잡 상태 폴링 */
    @GetMapping("/jobs/{jobId}/status")
    public ApiResponse<JobDto> getJobStatus(@PathVariable Long jobId) {
        return ApiResponse.ok(shortsService.getJobStatus(jobId));
    }

    // ── 트렌딩 토픽 ────────────────────────────────────────────────

    /** 트렌딩 토픽 제안 */
    @GetMapping("/trending/topics")
    public ApiResponse<List<Map<String, Object>>> getTrendingTopics(
            @AuthenticationPrincipal FirebaseToken token,
            @RequestParam(defaultValue = "stock") String category,
            @RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.ok(shortsService.getTrendingTopics(category, limit));
    }

    // ── 대본 히스토리 ───────────────────────────────────────────────

    /** 대본 히스토리 목록 */
    @GetMapping("/projects/{projectId}/script/history")
    public ApiResponse<List<com.crown.shorts.dto.ScriptHistoryDto>> getScriptHistory(
            @AuthenticationPrincipal FirebaseToken token,
            @PathVariable Long projectId) {
        Long memberId = memberService.findByGoogleId(token.getUid()).getMemberId();
        return ApiResponse.ok(shortsService.getScriptHistory(projectId, memberId));
    }

    /** 대본 히스토리 복원 */
    @PostMapping("/projects/{projectId}/script/history/{historyId}/restore")
    public ApiResponse<Void> restoreScriptHistory(
            @AuthenticationPrincipal FirebaseToken token,
            @PathVariable Long projectId,
            @PathVariable Long historyId) {
        Long memberId = memberService.findByGoogleId(token.getUid()).getMemberId();
        shortsService.restoreScriptHistory(projectId, memberId, historyId);
        return ApiResponse.ok(null);
    }

    // ── AI 기능 ─────────────────────────────────────────────────────

    /** AI 대본 재작성 */
    @PostMapping("/projects/{projectId}/ai/rewrite")
    public ApiResponse<String> rewriteScript(
            @AuthenticationPrincipal FirebaseToken token,
            @PathVariable Long projectId,
            @RequestBody Map<String, Object> body) {
        Long memberId = memberService.findByGoogleId(token.getUid()).getMemberId();
        return ApiResponse.ok(shortsService.rewriteScript(
                projectId, memberId,
                (String) body.get("text"),
                (String) body.getOrDefault("style", "news"),
                (String) body.getOrDefault("instruction", "")
        ));
    }

    /** AI 대본 번역 */
    @PostMapping("/projects/{projectId}/ai/translate")
    public ApiResponse<String> translateScript(
            @AuthenticationPrincipal FirebaseToken token,
            @PathVariable Long projectId,
            @RequestBody Map<String, Object> body) {
        Long memberId = memberService.findByGoogleId(token.getUid()).getMemberId();
        return ApiResponse.ok(shortsService.translateScript(
                projectId, memberId,
                (String) body.get("text"),
                (String) body.getOrDefault("target_language", "en")
        ));
    }

    // ── AI 강화 — 해시태그 / SEO / 품질 분석 ─────────────────────────

    /** AI 해시태그 생성 */
    @PostMapping("/projects/{projectId}/ai/hashtags")
    public ApiResponse<List<String>> generateHashtags(
            @AuthenticationPrincipal FirebaseToken token,
            @PathVariable Long projectId,
            @RequestBody Map<String, Object> body) {
        Long memberId = memberService.findByGoogleId(token.getUid()).getMemberId();
        return ApiResponse.ok(shortsService.generateHashtags(
                projectId, memberId,
                (String) body.getOrDefault("title",  ""),
                (String) body.getOrDefault("script", ""),
                body.containsKey("count") ? ((Number) body.get("count")).intValue() : 15
        ));
    }

    /** AI SEO 최적화 */
    @PostMapping("/projects/{projectId}/ai/seo")
    public ApiResponse<Map<String, Object>> generateSeo(
            @AuthenticationPrincipal FirebaseToken token,
            @PathVariable Long projectId,
            @RequestBody Map<String, Object> body) {
        Long memberId = memberService.findByGoogleId(token.getUid()).getMemberId();
        return ApiResponse.ok(shortsService.generateSeo(
                projectId, memberId,
                (String) body.getOrDefault("title",  ""),
                (String) body.getOrDefault("script", "")
        ));
    }

    /** AI 대본 품질 분석 */
    @PostMapping("/projects/{projectId}/ai/quality")
    public ApiResponse<Map<String, Object>> analyzeQuality(
            @AuthenticationPrincipal FirebaseToken token,
            @PathVariable Long projectId,
            @RequestBody Map<String, Object> body) {
        Long memberId = memberService.findByGoogleId(token.getUid()).getMemberId();
        return ApiResponse.ok(shortsService.analyzeQuality(
                projectId, memberId,
                (String) body.getOrDefault("script", "")
        ));
    }

    // ── 자막 ───────────────────────────────────────────────────────

    /** 대본 → SRT 자막 생성 */
    @PostMapping("/projects/{projectId}/subtitle/script")
    public ApiResponse<Map<String, Object>> subtitleFromScript(
            @AuthenticationPrincipal FirebaseToken token,
            @PathVariable Long projectId,
            @RequestBody Map<String, Object> body) {
        Long memberId = memberService.findByGoogleId(token.getUid()).getMemberId();
        @SuppressWarnings("unchecked")
        Map<String, String> script = (Map<String, String>) body.get("script");
        return ApiResponse.ok(shortsService.generateSubtitleFromScript(
                projectId, memberId, script,
                (String) body.getOrDefault("tts_rate", "+25%")
        ));
    }

    /** 영상 → Whisper 자막 생성 */
    @PostMapping("/projects/{projectId}/subtitle/video")
    public ApiResponse<Map<String, Object>> subtitleFromVideo(
            @AuthenticationPrincipal FirebaseToken token,
            @PathVariable Long projectId,
            @RequestBody Map<String, Object> body) {
        Long memberId = memberService.findByGoogleId(token.getUid()).getMemberId();
        return ApiResponse.ok(shortsService.generateSubtitleFromVideo(
                projectId, memberId,
                (String) body.get("video_url"),
                (String) body.getOrDefault("language", "ko")
        ));
    }

    /** SRT 자막 다국어 번역 */
    @PostMapping("/projects/{projectId}/subtitle/translate")
    public ApiResponse<Map<String, Object>> translateSubtitle(
            @AuthenticationPrincipal FirebaseToken token,
            @PathVariable Long projectId,
            @RequestBody Map<String, Object> body) {
        Long memberId = memberService.findByGoogleId(token.getUid()).getMemberId();
        return ApiResponse.ok(shortsService.translateSubtitle(
                projectId, memberId,
                (String) body.get("srt"),
                (String) body.getOrDefault("target_language", "en")
        ));
    }

    // ── 목소리 복제 ─────────────────────────────────────────────────

    /** 목소리 복제 생성 (오디오 샘플 업로드) */
    @PostMapping("/voice/clone")
    public ApiResponse<Map<String, Object>> cloneVoice(
            @AuthenticationPrincipal FirebaseToken token,
            @RequestParam("name") String name,
            @RequestParam(value = "description", defaultValue = "") String description,
            @RequestParam("sample") MultipartFile sample) throws IOException {
        return ApiResponse.ok(shortsService.cloneVoice(
                name, description, sample.getBytes(), sample.getOriginalFilename()));
    }

    /** 사용 가능한 목소리 목록 */
    @GetMapping("/voice/list")
    public ApiResponse<List<Map<String, Object>>> listVoices(
            @AuthenticationPrincipal FirebaseToken token) {
        return ApiResponse.ok(shortsService.listVoices());
    }

    /** 목소리 복제 삭제 */
    @DeleteMapping("/voice/{voiceId}")
    public ApiResponse<Void> deleteVoice(
            @AuthenticationPrincipal FirebaseToken token,
            @PathVariable String voiceId) {
        shortsService.deleteVoice(voiceId);
        return ApiResponse.ok(null);
    }

    /** 클립 미디어(이미지/영상) S3 업로드 */
    @PostMapping("/projects/{projectId}/upload-asset")
    public ApiResponse<String> uploadAsset(
            @AuthenticationPrincipal FirebaseToken token,
            @PathVariable Long projectId,
            @RequestParam("file") MultipartFile file) throws IOException {
        Long memberId = memberService.findByGoogleId(token.getUid()).getMemberId();
        String url = shortsService.uploadAsset(
                projectId, memberId,
                file.getBytes(), file.getOriginalFilename(), file.getContentType());
        return ApiResponse.ok(url);
    }

    // ── PPT 슬라이드 AI 생성 ───────────────────────────────────────

    @PostMapping("/projects/{projectId}/generate-ppt")
    public ApiResponse<Void> generatePptSlides(
            @AuthenticationPrincipal FirebaseToken token,
            @PathVariable Long projectId,
            @RequestBody(required = false) Map<String, Object> body) {
        Long memberId = memberService.findByGoogleId(token.getUid()).getMemberId();
        shortsService.generatePptSlides(projectId, memberId, body);
        return ApiResponse.ok(null);
    }

    // ── PDF / PPTX 내보내기 ────────────────────────────────────────

    @PostMapping("/projects/{projectId}/export/pdf")
    public ApiResponse<Map<String, String>> exportPdf(
            @AuthenticationPrincipal FirebaseToken token,
            @PathVariable Long projectId) {
        Long memberId = memberService.findByGoogleId(token.getUid()).getMemberId();
        String url = shortsService.exportPdf(projectId, memberId);
        return ApiResponse.ok(Map.of("url", url));
    }

    @PostMapping("/projects/{projectId}/export/pptx")
    public ApiResponse<Map<String, String>> exportPptx(
            @AuthenticationPrincipal FirebaseToken token,
            @PathVariable Long projectId) {
        Long memberId = memberService.findByGoogleId(token.getUid()).getMemberId();
        String url = shortsService.exportPptx(projectId, memberId);
        return ApiResponse.ok(Map.of("url", url));
    }

    // ── Python 워커 콜백 (내부 전용) ────────────────────────────────

    @PutMapping("/internal/generate-callback/{projectId}")
    public void generateCallback(
            @PathVariable Long projectId,
            @RequestBody Map<String, Object> body) {
        String status = (String) body.get("status");
        if ("done".equals(status)) {
            @SuppressWarnings("unchecked")
            Map<String, String> script = (Map<String, String>) body.get("script");
            shortsService.onGenerateDone(
                    projectId,
                    (String) body.get("html_url"),
                    script,
                    (String) body.getOrDefault("title", ""),
                    (String) body.get("thumbnail_url")
            );
        } else {
            shortsService.onGenerateError(projectId, (String) body.get("error_message"));
        }
    }

    @PutMapping("/internal/render-callback/{jobId}")
    public void renderCallback(
            @PathVariable Long jobId,
            @RequestParam Long projectId,
            @RequestBody Map<String, Object> body) {
        String status = (String) body.get("status");
        if ("done".equals(status)) {
            shortsService.onRenderDone(jobId, projectId, (String) body.get("video_url"), (String) body.get("thumbnail_url"));
        } else {
            shortsService.onRenderError(jobId, projectId, (String) body.get("error_message"));
        }
    }

    /** Python 워커 → 진행률 브로드캐스트 */
    @PostMapping("/internal/progress/{projectId}")
    public void receiveProgress(
            @PathVariable Long projectId,
            @RequestBody Map<String, Object> body) {
        Long jobId = body.get("job_id") != null ? Long.valueOf(body.get("job_id").toString()) : null;
        int percent = body.get("percent") != null ? (int) body.get("percent") : 0;
        String step    = (String) body.getOrDefault("step", "");
        String message = (String) body.getOrDefault("message", "");
        progressService.send(projectId, jobId, percent, step, message);
    }

    // ── 사용량 조회 ─────────────────────────────────────────────────

    /** 이번 달 사용량 조회 */
    @GetMapping("/usage")
    public ApiResponse<Map<String, Object>> getUsage(@AuthenticationPrincipal FirebaseToken token) {
        Long memberId = memberService.findByGoogleId(token.getUid()).getMemberId();
        String plan = memberService.findById(memberId).getPlan();
        int generateCount = usageLimitService.getMonthlyUsage(memberId, "GENERATE");
        int renderCount   = usageLimitService.getMonthlyUsage(memberId, "RENDER");
        return ApiResponse.ok(Map.of(
                "plan",          plan != null ? plan : "free",
                "limit",         UsageLimitService.FREE_MONTHLY_LIMIT,
                "generateCount", generateCount,
                "renderCount",   renderCount
        ));
    }

    // ── 배치 스케줄 ─────────────────────────────────────────────────

    /** 내 스케줄 목록 */
    @GetMapping("/schedule")
    public ApiResponse<List<Map<String, Object>>> getSchedules(@AuthenticationPrincipal FirebaseToken token) {
        Long memberId = memberService.findByGoogleId(token.getUid()).getMemberId();
        return ApiResponse.ok(shortsService.getSchedules(memberId));
    }

    /** 스케줄 생성 */
    @PostMapping("/schedule")
    public ApiResponse<Map<String, Object>> createSchedule(
            @AuthenticationPrincipal FirebaseToken token,
            @RequestBody Map<String, Object> body) {
        Long memberId = memberService.findByGoogleId(token.getUid()).getMemberId();
        return ApiResponse.ok(shortsService.createSchedule(memberId, body));
    }

    /** 스케줄 단건 조회 */
    @GetMapping("/schedule/{id}")
    public ApiResponse<Map<String, Object>> getSchedule(
            @AuthenticationPrincipal FirebaseToken token,
            @PathVariable Long id) {
        memberService.findByGoogleId(token.getUid()); // 인증 확인
        return ApiResponse.ok(shortsService.getSchedule(id));
    }

    /** 스케줄 수정 */
    @PutMapping("/schedule/{id}")
    public ApiResponse<Map<String, Object>> updateSchedule(
            @AuthenticationPrincipal FirebaseToken token,
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        memberService.findByGoogleId(token.getUid());
        return ApiResponse.ok(shortsService.updateSchedule(id, body));
    }

    /** 스케줄 삭제 */
    @DeleteMapping("/schedule/{id}")
    public ApiResponse<Void> deleteSchedule(
            @AuthenticationPrincipal FirebaseToken token,
            @PathVariable Long id) {
        memberService.findByGoogleId(token.getUid());
        shortsService.deleteSchedule(id);
        return ApiResponse.ok(null);
    }

    /** 스케줄 활성/비활성 토글 */
    @PatchMapping("/schedule/{id}/toggle")
    public ApiResponse<Map<String, Object>> toggleSchedule(
            @AuthenticationPrincipal FirebaseToken token,
            @PathVariable Long id) {
        memberService.findByGoogleId(token.getUid());
        return ApiResponse.ok(shortsService.toggleSchedule(id));
    }

    // ── 코멘트/피드백 ────────────────────────────────────────────────

    /** 프로젝트 코멘트 목록 */
    @GetMapping("/projects/{projectId}/comments")
    public ApiResponse<List<Map<String, Object>>> getComments(
            @AuthenticationPrincipal FirebaseToken token,
            @PathVariable Long projectId) {
        memberService.findByGoogleId(token.getUid());
        return ApiResponse.ok(shortsService.getComments(projectId));
    }

    /** 코멘트 생성 */
    @PostMapping("/projects/{projectId}/comments")
    public ApiResponse<Map<String, Object>> createComment(
            @AuthenticationPrincipal FirebaseToken token,
            @PathVariable Long projectId,
            @RequestBody Map<String, Object> body) {
        Long memberId = memberService.findByGoogleId(token.getUid()).getMemberId();
        body = new java.util.HashMap<>(body);
        body.put("project_id", projectId);
        return ApiResponse.ok(shortsService.createComment(memberId, body));
    }

    /** 코멘트 수정 (작성자만) */
    @PutMapping("/comments/{commentId}")
    public ApiResponse<Map<String, Object>> updateComment(
            @AuthenticationPrincipal FirebaseToken token,
            @PathVariable Long commentId,
            @RequestBody Map<String, Object> body) {
        memberService.findByGoogleId(token.getUid());
        String content = (String) body.get("content");
        return ApiResponse.ok(shortsService.updateComment(commentId, content));
    }

    /** 코멘트 삭제 (작성자만) */
    @DeleteMapping("/comments/{commentId}")
    public ApiResponse<Void> deleteComment(
            @AuthenticationPrincipal FirebaseToken token,
            @PathVariable Long commentId) {
        memberService.findByGoogleId(token.getUid());
        shortsService.deleteComment(commentId);
        return ApiResponse.ok(null);
    }

    // ── 팀 협업 ─────────────────────────────────────────────────────

    /** 내 팀 목록 */
    @GetMapping("/teams")
    public ApiResponse<List<Map<String, Object>>> getTeams(
            @AuthenticationPrincipal FirebaseToken token) {
        Long memberId = memberService.findByGoogleId(token.getUid()).getMemberId();
        return ApiResponse.ok(shortsService.getTeams(memberId));
    }

    /** 팀 생성 */
    @PostMapping("/teams")
    public ApiResponse<Map<String, Object>> createTeam(
            @AuthenticationPrincipal FirebaseToken token,
            @RequestBody Map<String, Object> body) {
        Long memberId = memberService.findByGoogleId(token.getUid()).getMemberId();
        return ApiResponse.ok(shortsService.createTeam(memberId, body));
    }

    /** 팀 상세 (멤버 포함) */
    @GetMapping("/teams/{teamId}")
    public ApiResponse<Map<String, Object>> getTeam(
            @AuthenticationPrincipal FirebaseToken token,
            @PathVariable Long teamId) {
        memberService.findByGoogleId(token.getUid());
        return ApiResponse.ok(shortsService.getTeam(teamId));
    }

    /** 팀 삭제 (owner 전용) */
    @DeleteMapping("/teams/{teamId}")
    public ApiResponse<Void> deleteTeam(
            @AuthenticationPrincipal FirebaseToken token,
            @PathVariable Long teamId) {
        memberService.findByGoogleId(token.getUid());
        shortsService.deleteTeam(teamId);
        return ApiResponse.ok(null);
    }

    /** 멤버 초대 */
    @PostMapping("/teams/{teamId}/invite")
    public ApiResponse<Map<String, Object>> inviteTeamMember(
            @AuthenticationPrincipal FirebaseToken token,
            @PathVariable Long teamId,
            @RequestBody Map<String, Object> body) {
        memberService.findByGoogleId(token.getUid());
        return ApiResponse.ok(shortsService.inviteTeamMember(teamId, body));
    }

    /** 역할 변경 */
    @PutMapping("/teams/{teamId}/members/{memberId}/role")
    public ApiResponse<Map<String, Object>> updateTeamMemberRole(
            @AuthenticationPrincipal FirebaseToken token,
            @PathVariable Long teamId,
            @PathVariable Long memberId,
            @RequestBody Map<String, Object> body) {
        memberService.findByGoogleId(token.getUid());
        String role = (String) body.get("role");
        return ApiResponse.ok(shortsService.updateTeamMemberRole(teamId, memberId, role));
    }

    /** 멤버 제거 */
    @DeleteMapping("/teams/{teamId}/members/{memberId}")
    public ApiResponse<Void> removeTeamMember(
            @AuthenticationPrincipal FirebaseToken token,
            @PathVariable Long teamId,
            @PathVariable Long memberId) {
        memberService.findByGoogleId(token.getUid());
        shortsService.removeTeamMember(teamId, memberId);
        return ApiResponse.ok(null);
    }

    /** 초대 수락 */
    @PutMapping("/teams/{teamId}/accept")
    public ApiResponse<Map<String, Object>> acceptTeamInvite(
            @AuthenticationPrincipal FirebaseToken token,
            @PathVariable Long teamId) {
        Long memberId = memberService.findByGoogleId(token.getUid()).getMemberId();
        return ApiResponse.ok(shortsService.acceptTeamInvite(teamId, memberId));
    }

    /** 팀에 프로젝트 공유 */
    @PostMapping("/teams/{teamId}/projects")
    public ApiResponse<Map<String, Object>> shareTeamProject(
            @AuthenticationPrincipal FirebaseToken token,
            @PathVariable Long teamId,
            @RequestBody Map<String, Object> body) {
        Long memberId = memberService.findByGoogleId(token.getUid()).getMemberId();
        return ApiResponse.ok(shortsService.shareTeamProject(teamId, memberId, body));
    }

    /** 팀 공유 프로젝트 목록 */
    @GetMapping("/teams/{teamId}/projects")
    public ApiResponse<List<Map<String, Object>>> getTeamProjects(
            @AuthenticationPrincipal FirebaseToken token,
            @PathVariable Long teamId) {
        memberService.findByGoogleId(token.getUid());
        return ApiResponse.ok(shortsService.getTeamProjects(teamId));
    }

    /** 공유 해제 */
    @DeleteMapping("/teams/{teamId}/projects/{projectId}")
    public ApiResponse<Void> removeTeamProject(
            @AuthenticationPrincipal FirebaseToken token,
            @PathVariable Long teamId,
            @PathVariable Long projectId) {
        memberService.findByGoogleId(token.getUid());
        shortsService.removeTeamProject(teamId, projectId);
        return ApiResponse.ok(null);
    }

    // ── 예외 처리 ───────────────────────────────────────────────────

    /** 사용량 초과 시 402 Payment Required */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleUsageLimit(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                .body(ApiResponse.fail(ex.getMessage()));
    }
}
