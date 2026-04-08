package com.crown.blog.mapper;

import com.crown.blog.dto.BlogPostDto;
import com.crown.blog.dto.BlogToneDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BlogMapper {

    // ── Tone ──
    BlogToneDto selectToneByMemberId(@Param("memberId") Long memberId);

    void insertTone(BlogToneDto tone);

    void updateTone(BlogToneDto tone);

    // ── Post ──
    void insertPost(BlogPostDto post);

    BlogPostDto selectPostById(@Param("postId") Long postId);

    List<BlogPostDto> selectPostsByMemberId(@Param("memberId") Long memberId,
                                            @Param("status") String status);

    void updatePostContent(@Param("postId") Long postId,
                           @Param("content") String content,
                           @Param("status") String status);

    void updatePostSubject(@Param("postId") Long postId,
                           @Param("subject") String subject);

    void updatePostStatus(@Param("postId") Long postId,
                          @Param("status") String status,
                          @Param("errorMessage") String errorMessage);

    void updatePostPublished(@Param("postId") Long postId,
                             @Param("publishedUrl") String publishedUrl,
                             @Param("status") String status);

    void updatePostSchedule(@Param("postId") Long postId,
                            @Param("scheduledAt") String scheduledAt,
                            @Param("status") String status);

    void deletePost(@Param("postId") Long postId);

    List<BlogPostDto> selectScheduledPosts();
}
