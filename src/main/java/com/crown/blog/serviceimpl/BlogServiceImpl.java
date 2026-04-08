package com.crown.blog.serviceimpl;

import com.crown.blog.dao.BlogDao;
import com.crown.blog.dto.BlogPostDto;
import com.crown.blog.dto.BlogToneDto;
import com.crown.blog.service.BlogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class BlogServiceImpl implements BlogService {

    private final BlogDao blogDao;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${worker.url}")
    private String workerUrl;

    @Value("${worker.secret}")
    private String workerSecret;

    @Value("${app.base-url}")
    private String appBaseUrl;

    @Value("${gemini.api-key:}")
    private String geminiApiKey;

    // ══════════════════════════════════════════════════════════════
    // Tone
    // ══════════════════════════════════════════════════════════════

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> analyzeTone(String sampleText, String quickTone) {
        // Gemini로 말투 분석
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            log.warn("[Blog] GEMINI_API_KEY 미설정 — 폴백 분석 사용");
            return fallbackAnalysis(quickTone);
        }

        try {
            String prompt = buildToneAnalysisPrompt(sampleText, quickTone);
            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + geminiApiKey;

            Map<String, Object> body = Map.of(
                "contents", List.of(Map.of(
                    "parts", List.of(Map.of("text", prompt))
                ))
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<Map> res = restTemplate.exchange(
                url, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);

            if (res.getBody() == null) return fallbackAnalysis(quickTone);

            List<Map<String, Object>> candidates = (List<Map<String, Object>>) res.getBody().get("candidates");
            if (candidates == null || candidates.isEmpty()) return fallbackAnalysis(quickTone);

            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            String text = (String) parts.get(0).get("text");

            return parseToneAnalysis(text);
        } catch (Exception e) {
            log.warn("[Blog] Gemini 말투 분석 실패: {}", e.getMessage());
            return fallbackAnalysis(quickTone);
        }
    }

    private String buildToneAnalysisPrompt(String sampleText, String quickTone) {
        String hint = quickTone != null ? "\n사용자가 선택한 말투 힌트: " + quickTone : "";
        return """
            당신은 블로그 글쓰기 스타일 분석 전문가입니다.
            아래 샘플 텍스트의 글쓰기 스타일을 분석해주세요.%s

            샘플 텍스트:
            %s

            반드시 아래 JSON 형식으로만 응답해주세요 (다른 텍스트 없이):
            {"style": "스타일명 (예: 친근하면서 전문적인)", "characteristics": ["특징1", "특징2", "특징3", "특징4"]}

            특징은 3~5개, 각 특징은 10자 이내로 간결하게 작성해주세요.
            """.formatted(hint, sampleText);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseToneAnalysis(String text) {
        try {
            // JSON 블록 추출
            String json = text;
            if (text.contains("```")) {
                int start = text.indexOf("{");
                int end = text.lastIndexOf("}");
                if (start >= 0 && end > start) json = text.substring(start, end + 1);
            } else if (text.contains("{")) {
                int start = text.indexOf("{");
                int end = text.lastIndexOf("}");
                if (start >= 0 && end > start) json = text.substring(start, end + 1);
            }
            Map<String, Object> result = objectMapper.readValue(json, Map.class);
            if (result.containsKey("style") && result.containsKey("characteristics")) {
                return result;
            }
        } catch (Exception e) {
            log.warn("[Blog] 말투 분석 결과 파싱 실패: {}", e.getMessage());
        }
        return fallbackAnalysis(null);
    }

    private Map<String, Object> fallbackAnalysis(String quickTone) {
        if ("friendly".equals(quickTone)) {
            return Map.of("style", "친근한 대화체", "characteristics", List.of("~요 체 사용", "이모지 활용", "짧은 문장", "구어체"));
        } else if ("professional".equals(quickTone)) {
            return Map.of("style", "전문적인 분석체", "characteristics", List.of("데이터 인용", "객관적 서술", "~합니다 체", "논리적 구조"));
        } else if ("humorous".equals(quickTone)) {
            return Map.of("style", "유머러스한 일상체", "characteristics", List.of("ㅋㅋ 사용", "과장 표현", "짧은 호흡", "공감 유도"));
        } else if ("emotional".equals(quickTone)) {
            return Map.of("style", "감성적인 서정체", "characteristics", List.of("비유 표현", "긴 문장", "감각적 묘사", "여운 있는 마무리"));
        } else if ("concise".equals(quickTone)) {
            return Map.of("style", "간결한 핵심 전달체", "characteristics", List.of("짧은 문장", "명사형 종결", "핵심만 전달", "불필요한 수식 제거"));
        } else if ("formal".equals(quickTone)) {
            return Map.of("style", "격식 있는 존댓말체", "characteristics", List.of("~하십니다 체", "한자어 활용", "단정한 문장", "공식 어투"));
        }
        return Map.of("style", "자연스러운 블로그체", "characteristics", List.of("~요 체 사용", "적절한 구어체", "문단 나누기", "독자 소통"));
    }

    @Override
    public BlogToneDto getToneProfile(Long memberId) {
        return blogDao.getToneByMemberId(memberId);
    }

    @Override
    public BlogToneDto saveToneProfile(Long memberId, String style, List<String> characteristics, String sampleText) {
        BlogToneDto tone = new BlogToneDto();
        tone.setMemberId(memberId);
        tone.setStyle(style);
        tone.setCharacteristics(characteristics);
        tone.setSampleText(sampleText);
        return blogDao.saveTone(tone);
    }

    // ══════════════════════════════════════════════════════════════
    // Post
    // ══════════════════════════════════════════════════════════════

    @Override
    public BlogPostDto createPost(Long memberId, String subject, List<String> mediaUrls,
                                  String additionalInfo, String platform, String scheduledAt) {
        BlogPostDto post = new BlogPostDto();
        post.setMemberId(memberId);
        post.setSubject(subject);
        post.setMediaUrls(mediaUrls);
        post.setAdditionalInfo(additionalInfo);
        post.setPlatform(platform != null ? platform : "naver");
        post.setStatus("generating");

        BlogPostDto saved = blogDao.createPost(post);

        // 비동기로 워커에 글 생성 요청
        requestGenerate(saved.getPostId(), memberId, subject, mediaUrls, additionalInfo, platform);

        return saved;
    }

    @Async
    protected void requestGenerate(Long postId, Long memberId, String subject,
                                   List<String> mediaUrls, String additionalInfo, String platform) {
        try {
            BlogToneDto tone = blogDao.getToneByMemberId(memberId);

            String callbackUrl = appBaseUrl + "/api/blog/internal/generate-callback/" + postId;
            String errorCallbackUrl = appBaseUrl + "/api/blog/internal/generate-error/" + postId;

            Map<String, Object> body = new HashMap<>();
            body.put("post_id", postId);
            body.put("subject", subject);
            body.put("platform", platform);
            body.put("callback_url", callbackUrl);
            body.put("error_callback_url", errorCallbackUrl);

            if (mediaUrls != null && !mediaUrls.isEmpty()) {
                body.put("media_urls", mediaUrls);
            }
            if (additionalInfo != null && !additionalInfo.isBlank()) {
                body.put("additional_info", additionalInfo);
            }
            if (tone != null) {
                body.put("tone_profile", Map.of(
                    "style", tone.getStyle(),
                    "characteristics", tone.getCharacteristics(),
                    "sample_text", tone.getSampleText()
                ));
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Worker-Secret", workerSecret);

            restTemplate.exchange(
                workerUrl + "/blog/generate",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                Map.class
            );

            log.info("[Blog] 글 생성 요청 완료: postId={}", postId);
        } catch (Exception e) {
            log.error("[Blog] 글 생성 요청 실패: postId={}, error={}", postId, e.getMessage());
            blogDao.updatePostStatus(postId, "error", "글 생성 요청 실패: " + e.getMessage());
        }
    }

    @Override
    public List<BlogPostDto> getPosts(Long memberId, String status) {
        return blogDao.getPostsByMemberId(memberId, status);
    }

    @Override
    public BlogPostDto getPost(Long postId) {
        return blogDao.getPostById(postId);
    }

    @Override
    public void updatePost(Long postId, String subject, String content) {
        if (subject != null) blogDao.updatePostSubject(postId, subject);
        if (content != null) blogDao.updatePostContent(postId, content, "ready");
    }

    @Override
    public void deletePost(Long postId, Long memberId) {
        BlogPostDto post = blogDao.getPostById(postId);
        if (post == null) throw new IllegalArgumentException("글을 찾을 수 없습니다.");
        if (!post.getMemberId().equals(memberId)) throw new SecurityException("권한이 없습니다.");
        blogDao.deletePost(postId);
    }

    // ══════════════════════════════════════════════════════════════
    // Publish
    // ══════════════════════════════════════════════════════════════

    @Override
    public void publishPost(Long postId, String platform) {
        BlogPostDto post = blogDao.getPostById(postId);
        if (post == null) throw new IllegalArgumentException("글을 찾을 수 없습니다.");
        if (post.getContent() == null || post.getContent().isBlank()) {
            throw new IllegalStateException("발행할 콘텐츠가 없습니다.");
        }

        blogDao.updatePostStatus(postId, "generating", null);
        requestPublish(postId, post, platform);
    }

    @Async
    protected void requestPublish(Long postId, BlogPostDto post, String platform) {
        try {
            String callbackUrl = appBaseUrl + "/api/blog/internal/publish-callback/" + postId;
            String errorCallbackUrl = appBaseUrl + "/api/blog/internal/publish-error/" + postId;

            Map<String, Object> body = new HashMap<>();
            body.put("post_id", postId);
            body.put("platform", platform != null ? platform : post.getPlatform());
            body.put("subject", post.getSubject());
            body.put("content", post.getContent());
            body.put("callback_url", callbackUrl);
            body.put("error_callback_url", errorCallbackUrl);

            if (post.getMediaUrls() != null && !post.getMediaUrls().isEmpty()) {
                body.put("media_urls", post.getMediaUrls());
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Worker-Secret", workerSecret);

            restTemplate.exchange(
                workerUrl + "/blog/publish",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                Map.class
            );

            log.info("[Blog] 발행 요청 완료: postId={}, platform={}", postId, platform);
        } catch (Exception e) {
            log.error("[Blog] 발행 요청 실패: postId={}, error={}", postId, e.getMessage());
            blogDao.updatePostStatus(postId, "error", "발행 요청 실패: " + e.getMessage());
        }
    }

    @Override
    public void schedulePost(Long postId, String platform, String scheduledAt) {
        BlogPostDto post = blogDao.getPostById(postId);
        if (post == null) throw new IllegalArgumentException("글을 찾을 수 없습니다.");
        blogDao.updatePostSchedule(postId, scheduledAt);
    }

    @Override
    public void cancelSchedule(Long postId) {
        blogDao.cancelSchedule(postId);
    }

    // ══════════════════════════════════════════════════════════════
    // Internal callbacks (from python-worker)
    // ══════════════════════════════════════════════════════════════

    @Override
    public void onGenerateComplete(Long postId, String content) {
        blogDao.updatePostContent(postId, content, "ready");
        log.info("[Blog] 글 생성 완료: postId={}", postId);
    }

    @Override
    public void onGenerateError(Long postId, String errorMessage) {
        blogDao.updatePostStatus(postId, "error", errorMessage);
        log.error("[Blog] 글 생성 오류: postId={}, error={}", postId, errorMessage);
    }

    @Override
    public void onPublishComplete(Long postId, String publishedUrl) {
        blogDao.updatePostPublished(postId, publishedUrl);
        log.info("[Blog] 발행 완료: postId={}, url={}", postId, publishedUrl);
    }

    @Override
    public void onPublishError(Long postId, String errorMessage) {
        blogDao.updatePostStatus(postId, "error", errorMessage);
        log.error("[Blog] 발행 오류: postId={}, error={}", postId, errorMessage);
    }
}
