package com.crown.blog.restcontroller;

import com.crown.blog.dto.BlogPostDto;
import com.crown.blog.dto.BlogToneDto;
import com.crown.blog.service.BlogService;
import com.crown.common.dto.ApiResponse;
import com.crown.member.service.MemberService;
import com.google.firebase.auth.FirebaseToken;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/blog")
@RequiredArgsConstructor
public class BlogRestController {

    private final BlogService blogService;
    private final MemberService memberService;

    private Long getMemberId(FirebaseToken token) {
        return memberService.findByGoogleId(token.getUid()).getMemberId();
    }

    // ══════════════════════════════════════════════════════════════
    // Tone
    // ══════════════════════════════════════════════════════════════

    @PostMapping("/tone/analyze")
    public ApiResponse<Map<String, Object>> analyzeTone(
            @AuthenticationPrincipal FirebaseToken token,
            @RequestBody Map<String, String> body) {
        getMemberId(token); // 인증 확인
        String sampleText = body.get("sample_text");
        String quickTone = body.get("quick_tone");
        Map<String, Object> result = blogService.analyzeTone(sampleText, quickTone);
        return ApiResponse.ok(result);
    }

    @GetMapping("/tone")
    public ApiResponse<BlogToneDto> getToneProfile(@AuthenticationPrincipal FirebaseToken token) {
        Long memberId = getMemberId(token);
        BlogToneDto tone = blogService.getToneProfile(memberId);
        return ApiResponse.ok(tone); // null이면 미설정
    }

    @PostMapping("/tone")
    @SuppressWarnings("unchecked")
    public ApiResponse<BlogToneDto> saveToneProfile(
            @AuthenticationPrincipal FirebaseToken token,
            @RequestBody Map<String, Object> body) {
        Long memberId = getMemberId(token);
        String style = (String) body.get("style");
        List<String> characteristics = (List<String>) body.get("characteristics");
        String sampleText = (String) body.get("sampleText");
        BlogToneDto saved = blogService.saveToneProfile(memberId, style, characteristics, sampleText);
        return ApiResponse.ok(saved);
    }

    @PutMapping("/tone")
    public ApiResponse<Map<String, Object>> updateToneByAnalysis(
            @AuthenticationPrincipal FirebaseToken token,
            @RequestBody Map<String, String> body) {
        Long memberId = getMemberId(token);
        String sampleText = body.get("sample_text");
        String quickTone = body.get("quick_tone");
        Map<String, Object> result = blogService.analyzeTone(sampleText, quickTone);

        @SuppressWarnings("unchecked")
        List<String> characteristics = (List<String>) result.get("characteristics");
        blogService.saveToneProfile(memberId, (String) result.get("style"), characteristics, sampleText);

        return ApiResponse.ok(result);
    }

    // ══════════════════════════════════════════════════════════════
    // Posts
    // ══════════════════════════════════════════════════════════════

    @PostMapping("/posts")
    @SuppressWarnings("unchecked")
    public ApiResponse<BlogPostDto> createPost(
            @AuthenticationPrincipal FirebaseToken token,
            @RequestBody Map<String, Object> body) {
        Long memberId = getMemberId(token);
        String subject = (String) body.get("subject");
        List<String> mediaUrls = (List<String>) body.get("mediaUrls");
        String additionalInfo = (String) body.get("additionalInfo");
        String platform = (String) body.get("platform");
        String scheduledAt = (String) body.get("scheduledAt");

        BlogPostDto post = blogService.createPost(memberId, subject, mediaUrls, additionalInfo, platform, scheduledAt);
        return ApiResponse.ok(post);
    }

    @GetMapping("/posts")
    public ApiResponse<List<BlogPostDto>> getPosts(
            @AuthenticationPrincipal FirebaseToken token,
            @RequestParam(required = false) String status) {
        Long memberId = getMemberId(token);
        return ApiResponse.ok(blogService.getPosts(memberId, status));
    }

    @GetMapping("/posts/{postId}")
    public ApiResponse<BlogPostDto> getPost(
            @AuthenticationPrincipal FirebaseToken token,
            @PathVariable Long postId) {
        getMemberId(token);
        BlogPostDto post = blogService.getPost(postId);
        if (post == null) throw new IllegalArgumentException("글을 찾을 수 없습니다.");
        return ApiResponse.ok(post);
    }

    @PutMapping("/posts/{postId}")
    public ApiResponse<Void> updatePost(
            @AuthenticationPrincipal FirebaseToken token,
            @PathVariable Long postId,
            @RequestBody Map<String, String> body) {
        getMemberId(token);
        blogService.updatePost(postId, body.get("subject"), body.get("content"));
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/posts/{postId}")
    public ApiResponse<Void> deletePost(
            @AuthenticationPrincipal FirebaseToken token,
            @PathVariable Long postId) {
        Long memberId = getMemberId(token);
        blogService.deletePost(postId, memberId);
        return ApiResponse.ok(null);
    }

    // ══════════════════════════════════════════════════════════════
    // Publish / Schedule
    // ══════════════════════════════════════════════════════════════

    @PostMapping("/posts/{postId}/publish")
    public ApiResponse<Void> publishPost(
            @AuthenticationPrincipal FirebaseToken token,
            @PathVariable Long postId,
            @RequestBody Map<String, String> body) {
        getMemberId(token);
        blogService.publishPost(postId, body.get("platform"));
        return ApiResponse.ok(null);
    }

    @PostMapping("/posts/{postId}/schedule")
    public ApiResponse<Void> schedulePost(
            @AuthenticationPrincipal FirebaseToken token,
            @PathVariable Long postId,
            @RequestBody Map<String, String> body) {
        getMemberId(token);
        blogService.schedulePost(postId, body.get("platform"), body.get("scheduled_at"));
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/posts/{postId}/schedule")
    public ApiResponse<Void> cancelSchedule(
            @AuthenticationPrincipal FirebaseToken token,
            @PathVariable Long postId) {
        getMemberId(token);
        blogService.cancelSchedule(postId);
        return ApiResponse.ok(null);
    }

    // ══════════════════════════════════════════════════════════════
    // Platform (stub — 실제 연결은 Phase 5/6에서 구현)
    // ══════════════════════════════════════════════════════════════

    @GetMapping("/platforms")
    public ApiResponse<Map<String, Boolean>> getPlatformStatus(
            @AuthenticationPrincipal FirebaseToken token) {
        getMemberId(token);
        // TODO: 실제 플랫폼 연결 상태 확인 (Phase 5/6)
        return ApiResponse.ok(Map.of("naver", false, "tistory", false));
    }

    @DeleteMapping("/platforms/{platform}")
    public ApiResponse<Void> disconnectPlatform(
            @AuthenticationPrincipal FirebaseToken token,
            @PathVariable String platform) {
        getMemberId(token);
        // TODO: 실제 플랫폼 연결 해제 (Phase 5/6)
        return ApiResponse.ok(null);
    }

    // ══════════════════════════════════════════════════════════════
    // Media upload (S3 via worker)
    // ══════════════════════════════════════════════════════════════

    @PostMapping("/upload")
    public ApiResponse<Map<String, String>> uploadMedia(
            @AuthenticationPrincipal FirebaseToken token,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) throws java.io.IOException {
        getMemberId(token);

        // 워커의 S3 업로드 사용
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.MULTIPART_FORM_DATA);

        org.springframework.util.LinkedMultiValueMap<String, Object> formBody = new org.springframework.util.LinkedMultiValueMap<>();
        formBody.add("file", new org.springframework.core.io.ByteArrayResource(file.getBytes()) {
            @Override public String getFilename() { return file.getOriginalFilename(); }
        });

        // 워커의 /storage/upload 엔드포인트 활용
        org.springframework.http.ResponseEntity<Map> res = new org.springframework.web.client.RestTemplate()
            .postForEntity(
                // workerUrl은 private 필드라 여기서 직접 사용 불가 → blogService에 위임하거나 직접 주입
                // 간단하게 처리: BlogService에 위임 불필요, Controller에서 직접 처리
                "http://localhost:8003/storage/upload",
                new org.springframework.http.HttpEntity<>(formBody, headers),
                Map.class
            );

        @SuppressWarnings("unchecked")
        Map<String, String> result = Map.of("url", String.valueOf(res.getBody().get("url")));
        return ApiResponse.ok(result);
    }

    // ══════════════════════════════════════════════════════════════
    // Internal callbacks (인증 불필요 — SecurityConfig에서 제외)
    // ══════════════════════════════════════════════════════════════

    @PutMapping("/internal/generate-callback/{postId}")
    public Map<String, Object> onGenerateCallback(
            @PathVariable Long postId,
            @RequestBody Map<String, String> body) {
        blogService.onGenerateComplete(postId, body.get("content"));
        return Map.of("success", true);
    }

    @PutMapping("/internal/generate-error/{postId}")
    public Map<String, Object> onGenerateError(
            @PathVariable Long postId,
            @RequestBody Map<String, String> body) {
        blogService.onGenerateError(postId, body.get("error"));
        return Map.of("success", true);
    }

    @PutMapping("/internal/publish-callback/{postId}")
    public Map<String, Object> onPublishCallback(
            @PathVariable Long postId,
            @RequestBody Map<String, String> body) {
        blogService.onPublishComplete(postId, body.get("published_url"));
        return Map.of("success", true);
    }

    @PutMapping("/internal/publish-error/{postId}")
    public Map<String, Object> onPublishError(
            @PathVariable Long postId,
            @RequestBody Map<String, String> body) {
        blogService.onPublishError(postId, body.get("error"));
        return Map.of("success", true);
    }
}
