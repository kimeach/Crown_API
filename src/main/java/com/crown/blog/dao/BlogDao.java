package com.crown.blog.dao;

import com.crown.blog.dto.BlogPostDto;
import com.crown.blog.dto.BlogToneDto;
import com.crown.blog.mapper.BlogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class BlogDao {

    private final BlogMapper blogMapper;

    // ── Tone ──

    public BlogToneDto getToneByMemberId(Long memberId) {
        return blogMapper.selectToneByMemberId(memberId);
    }

    public BlogToneDto saveTone(BlogToneDto tone) {
        BlogToneDto existing = blogMapper.selectToneByMemberId(tone.getMemberId());
        if (existing != null) {
            tone.setToneId(existing.getToneId());
            blogMapper.updateTone(tone);
        } else {
            blogMapper.insertTone(tone);
        }
        return blogMapper.selectToneByMemberId(tone.getMemberId());
    }

    // ── Post ──

    public BlogPostDto createPost(BlogPostDto post) {
        blogMapper.insertPost(post);
        return blogMapper.selectPostById(post.getPostId());
    }

    public BlogPostDto getPostById(Long postId) {
        return blogMapper.selectPostById(postId);
    }

    public List<BlogPostDto> getPostsByMemberId(Long memberId, String status) {
        return blogMapper.selectPostsByMemberId(memberId, status);
    }

    public void updatePostContent(Long postId, String content, String status) {
        blogMapper.updatePostContent(postId, content, status);
    }

    public void updatePostSubject(Long postId, String subject) {
        blogMapper.updatePostSubject(postId, subject);
    }

    public void updatePostStatus(Long postId, String status, String errorMessage) {
        blogMapper.updatePostStatus(postId, status, errorMessage);
    }

    public void updatePostPublished(Long postId, String publishedUrl) {
        blogMapper.updatePostPublished(postId, publishedUrl, "published");
    }

    public void updatePostSchedule(Long postId, String scheduledAt) {
        blogMapper.updatePostSchedule(postId, scheduledAt, "scheduled");
    }

    public void cancelSchedule(Long postId) {
        blogMapper.updatePostSchedule(postId, null, "ready");
    }

    public void deletePost(Long postId) {
        blogMapper.deletePost(postId);
    }

    public List<BlogPostDto> getScheduledPosts() {
        return blogMapper.selectScheduledPosts();
    }
}
