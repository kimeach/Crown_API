package com.crown.shorts.serviceimpl;

import com.crown.shorts.dao.ShortsDao;
import com.crown.shorts.dto.JobDto;
import com.crown.shorts.dto.ProjectDto;
import com.crown.shorts.dto.QuestionDto;
import com.crown.shorts.service.ShortsService;
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
    private final ObjectMapper objectMapper;

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
        ProjectDto project = shortsDao.createProject(memberId, category, templateId, options);

        String callbackUrl = appBaseUrl + "/api/shorts/internal/generate-callback/" + project.getProjectId();
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("project_id",   project.getProjectId());
        body.put("callback_url", callbackUrl);
        body.put("template_id",  templateId);
        if (options != null && !options.isEmpty()) {
            body.put("options", options);
        }
        callWorker("POST", "/generate/stock", body);
        shortsDao.updateProjectStatus(project.getProjectId(), "generating");
        project.setStatus("generating");
        return project;
    }

    @Override
    public ProjectDto createBlank(Long memberId) {
        return shortsDao.createProject(memberId, "blank", "dark_blue", null);
    }

    @Override
    public byte[] getTtsPreview(String text, String voice, String rate) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Worker-Secret", workerSecret);
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("text",  text);
        body.put("voice", voice != null ? voice : "ko-KR-InJoonNeural");
        body.put("rate",  rate  != null ? rate  : "+25%");
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
        if (project == null || !project.getMemberId().equals(memberId)) {
            throw new IllegalArgumentException("프로젝트를 찾을 수 없습니다.");
        }
        return project;
    }

    // ── 대본 수정 ──────────────────────────────────────────────────

    @Override
    public void updateScript(Long projectId, Long memberId, Map<String, String> script) {
        getProject(projectId, memberId); // 소유권 확인
        try {
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
        if (renderOptions != null && !renderOptions.isEmpty()) {
            body.put("render_options", renderOptions);
        }
        callWorker("POST", "/render", body);
        shortsDao.updateJobStarted(job.getJobId());
        return job;
    }

    @Override
    public JobDto getJobStatus(Long jobId) {
        return shortsDao.getJobById(jobId);
    }

    // ── 워커 콜백 ──────────────────────────────────────────────────

    @Override
    public void onGenerateDone(Long projectId, String htmlUrl, Map<String, String> script, String title) {
        try {
            String scriptJson = objectMapper.writeValueAsString(script);
            shortsDao.updateProjectGenerated(projectId, htmlUrl, scriptJson, title, "done");
        } catch (JsonProcessingException e) {
            log.error("대본 직렬화 실패", e);
            shortsDao.updateProjectStatus(projectId, "error");
        }
    }

    @Override
    public void onGenerateError(Long projectId, String errorMessage) {
        log.error("[project {}] 생성 실패: {}", projectId, errorMessage);
        shortsDao.updateProjectStatus(projectId, "error");
    }

    @Override
    public void onRenderDone(Long jobId, Long projectId, String videoUrl) {
        shortsDao.updateProjectVideo(projectId, videoUrl, "done");
        shortsDao.updateJobFinished(jobId, "done", null);
    }

    @Override
    public void onRenderError(Long jobId, Long projectId, String errorMessage) {
        log.error("[job {}] 렌더링 실패: {}", jobId, errorMessage);
        shortsDao.updateProjectStatus(projectId, "error");
        shortsDao.updateJobFinished(jobId, "error", errorMessage);
    }

    // ── AI 재작성 ───────────────────────────────────────────────────

    @Override
    public String rewriteScript(Long projectId, Long memberId, String text, String style, String instruction) {
        getProject(projectId, memberId);
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("text",        text);
        body.put("style",       style != null ? style : "news");
        body.put("instruction", instruction != null ? instruction : "");
        Map<String, Object> res = callWorkerJson("POST", "/ai/rewrite", body);
        Object data = res.get("data");
        return data != null ? data.toString() : "";
    }

    @Override
    public String translateScript(Long projectId, Long memberId, String text, String targetLanguage) {
        getProject(projectId, memberId);
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("text",            text);
        body.put("target_language", targetLanguage != null ? targetLanguage : "en");
        Map<String, Object> res = callWorkerJson("POST", "/ai/translate", body);
        Object data = res.get("data");
        return data != null ? data.toString() : "";
    }

    // ── 자막 생성 ───────────────────────────────────────────────────

    @Override
    public Map<String, Object> generateSubtitleFromScript(Long projectId, Long memberId, Map<String, String> script, String ttsRate) {
        getProject(projectId, memberId);
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("project_id", projectId);
        body.put("script",     script);
        body.put("tts_rate",   ttsRate != null ? ttsRate : "+25%");
        Map<String, Object> res = callWorkerJson("POST", "/subtitle/from-script", body);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) res.get("data");
        return data != null ? data : new java.util.HashMap<>();
    }

    @Override
    public Map<String, Object> generateSubtitleFromVideo(Long projectId, Long memberId, String videoUrl, String language) {
        getProject(projectId, memberId);
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("project_id", projectId);
        body.put("video_url",  videoUrl);
        body.put("language",   language != null ? language : "ko");
        Map<String, Object> res = callWorkerJson("POST", "/subtitle/from-video", body);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) res.get("data");
        return data != null ? data : new java.util.HashMap<>();
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
        try {
            Map<String, Object> res = callWorkerJson("GET", "/voice/list", null);
            Object data = res.get("data");
            if (data instanceof List) return (List<Map<String, Object>>) data;
            return new java.util.ArrayList<>();
        } catch (Exception e) {
            log.error("목소리 목록 조회 실패: {}", e.getMessage());
            return new java.util.ArrayList<>();
        }
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

    // ── 내부 유틸 ──────────────────────────────────────────────────

    private void callWorker(String method, String path, Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Worker-Secret", workerSecret);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            restTemplate.exchange(workerUrl + path, HttpMethod.valueOf(method), entity, String.class);
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
            ResponseEntity<Map> response = restTemplate.exchange(
                    workerUrl + path, HttpMethod.valueOf(method), entity, Map.class);
            return response.getBody() != null ? response.getBody() : new java.util.HashMap<>();
        } catch (Exception e) {
            log.error("Python 워커 호출 실패 [{}{}]: {}", method, path, e.getMessage());
            throw new RuntimeException("AI 서버에 연결할 수 없습니다.", e);
        }
    }
}
