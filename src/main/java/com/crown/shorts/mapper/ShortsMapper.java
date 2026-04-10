package com.crown.shorts.mapper;

import com.crown.shorts.dto.JobDto;
import com.crown.shorts.dto.ProjectDto;
import com.crown.shorts.dto.QuestionDto;
import com.crown.shorts.dto.ScriptHistoryDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ShortsMapper {

    void insertProject(ProjectDto project);

    List<QuestionDto> selectQuestions(@Param("category") String category);

    ProjectDto selectProjectById(@Param("projectId") Long projectId);

    List<ProjectDto> selectProjectsByMemberId(@Param("memberId") Long memberId);

    void updateProjectStatus(@Param("projectId") Long projectId,
                             @Param("status") String status);

    void updateProjectGenerated(@Param("projectId") Long projectId,
                                @Param("htmlUrl") String htmlUrl,
                                @Param("script") String scriptJson,
                                @Param("title") String title,
                                @Param("status") String status);

    void updateProjectScript(@Param("projectId") Long projectId,
                             @Param("script") String scriptJson);

    void updateProjectVideo(@Param("projectId") Long projectId,
                            @Param("videoUrl") String videoUrl,
                            @Param("status") String status);

    void updateProjectTitle(@Param("projectId") Long projectId,
                            @Param("title") String title);

    void updateProjectThumbnail(@Param("projectId") Long projectId,
                                @Param("thumbnailUrl") String thumbnailUrl);

    void insertScriptHistory(ScriptHistoryDto dto);

    List<ScriptHistoryDto> selectScriptHistory(@Param("projectId") Long projectId);

    ScriptHistoryDto selectScriptHistoryById(@Param("historyId") Long historyId);

    void deleteJobsByProjectId(@Param("projectId") Long projectId);

    void deleteProject(@Param("projectId") Long projectId);

    void insertJob(JobDto job);

    JobDto selectJobById(@Param("jobId") Long jobId);

    void updateJobStatus(@Param("jobId") Long jobId,
                         @Param("status") String status,
                         @Param("errorMessage") String errorMessage);

    void updateJobStarted(@Param("jobId") Long jobId);

    void updateJobFinished(@Param("jobId") Long jobId,
                           @Param("status") String status,
                           @Param("errorMessage") String errorMessage);

    // ── 프로젝트 카운트 ──
    int countProjectsByMemberId(@Param("memberId") Long memberId);

    // ── 팀 협업 접근 제어 ──
    String selectTeamRole(@Param("projectId") Long projectId,
                          @Param("memberId") Long memberId);
}
