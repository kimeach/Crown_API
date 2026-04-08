package com.crown.blog.service;

import com.crown.blog.dto.BlogPostDto;
import com.crown.blog.dto.BlogToneDto;

import java.util.List;
import java.util.Map;

public interface BlogService {

    // ── Tone ──
    Map<String, Object> analyzeTone(String sampleText, String quickTone);

    BlogToneDto getToneProfile(Long memberId);

    BlogToneDto saveToneProfile(Long memberId, String style, List<String> characteristics, String sampleText);

    // ── Post ──
    BlogPostDto createPost(Long memberId, String subject, List<String> mediaUrls,
                           String additionalInfo, String platform, String scheduledAt);

    List<BlogPostDto> getPosts(Long memberId, String status);

    BlogPostDto getPost(Long postId);

    void updatePost(Long postId, String subject, String content);

    void deletePost(Long postId, Long memberId);

    // ── Publish ──
    void publishPost(Long postId, String platform);

    void schedulePost(Long postId, String platform, String scheduledAt);

    void cancelSchedule(Long postId);

    // ── Internal callback ──
    void onGenerateComplete(Long postId, String content);

    void onGenerateError(Long postId, String errorMessage);

    void onPublishComplete(Long postId, String publishedUrl);

    void onPublishError(Long postId, String errorMessage);
}
