package com.crown.shorts.dao;

import com.crown.shorts.dto.JobDto;
import com.crown.shorts.dto.ProjectDto;
import com.crown.shorts.dto.QuestionDto;
import com.crown.shorts.dto.ScriptHistoryDto;
import com.crown.shorts.mapper.ShortsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ShortsDao {

    private final ShortsMapper shortsMapper;

    public ProjectDto createProject(Long memberId, String category, String templateId, java.util.Map<String, Object> options) {
        ProjectDto dto = new ProjectDto();
        dto.setMemberId(memberId);
        dto.setCategory(category);
        dto.setTemplateId(templateId != null ? templateId : "dark_blue");
        dto.setOptions(options);
        shortsMapper.insertProject(dto);
        return dto;
    }

    public List<QuestionDto> getQuestions(String category) {
        return shortsMapper.selectQuestions(category);
    }

    public ProjectDto getProjectById(Long projectId) {
        return shortsMapper.selectProjectById(projectId);
    }

    public List<ProjectDto> getProjectsByMemberId(Long memberId) {
        return shortsMapper.selectProjectsByMemberId(memberId);
    }

    public void updateProjectStatus(Long projectId, String status) {
        shortsMapper.updateProjectStatus(projectId, status);
    }

    public void updateProjectGenerated(Long projectId, String htmlUrl, String scriptJson, String title, String status) {
        shortsMapper.updateProjectGenerated(projectId, htmlUrl, scriptJson, title, status);
    }

    public void updateProjectScript(Long projectId, String scriptJson) {
        shortsMapper.updateProjectScript(projectId, scriptJson);
    }

    public void updateProjectVideo(Long projectId, String videoUrl, String status) {
        shortsMapper.updateProjectVideo(projectId, videoUrl, status);
    }

    public void updateProjectTitle(Long projectId, String title) {
        shortsMapper.updateProjectTitle(projectId, title);
    }

    public void updateProjectThumbnail(Long projectId, String thumbnailUrl) {
        shortsMapper.updateProjectThumbnail(projectId, thumbnailUrl);
    }

    public void deleteProject(Long projectId) {
        shortsMapper.deleteJobsByProjectId(projectId);
        shortsMapper.deleteProject(projectId);
    }

    public ProjectDto duplicateProject(Long projectId, Long memberId) {
        ProjectDto src = shortsMapper.selectProjectById(projectId);
        if (src == null) throw new IllegalArgumentException("원본 프로젝트를 찾을 수 없습니다.");
        ProjectDto copy = new ProjectDto();
        copy.setMemberId(memberId);
        copy.setCategory(src.getCategory());
        copy.setTemplateId(src.getTemplateId());
        copy.setOptions(src.getOptions());
        copy.setTitle((src.getTitle() != null ? src.getTitle() : "제목 없음") + " 복사");
        copy.setHtmlUrl(src.getHtmlUrl());
        shortsMapper.insertProject(copy);
        // script는 별도 업데이트 (insertProject가 script를 다루지 않을 수 있으므로)
        if (src.getScript() != null && !src.getScript().isEmpty()) {
            try {
                String scriptJson = new com.fasterxml.jackson.databind.ObjectMapper()
                        .writeValueAsString(src.getScript());
                shortsMapper.updateProjectScript(copy.getProjectId(), scriptJson);
            } catch (Exception ignored) {}
        }
        copy.setScript(src.getScript());
        copy.setStatus("draft");
        return copy;
    }

    public ScriptHistoryDto saveScriptHistory(Long projectId, Long memberId, java.util.Map<String, String> script, String note) {
        ScriptHistoryDto dto = new ScriptHistoryDto();
        dto.setProjectId(projectId);
        dto.setMemberId(memberId);
        dto.setScript(script);
        dto.setNote(note);
        shortsMapper.insertScriptHistory(dto);
        return dto;
    }

    public java.util.List<ScriptHistoryDto> getScriptHistory(Long projectId) {
        return shortsMapper.selectScriptHistory(projectId);
    }

    public ScriptHistoryDto getScriptHistoryById(Long historyId) {
        return shortsMapper.selectScriptHistoryById(historyId);
    }

    public JobDto createJob(Long projectId) {
        JobDto dto = new JobDto();
        dto.setProjectId(projectId);
        shortsMapper.insertJob(dto);
        return dto;
    }

    public JobDto getJobById(Long jobId) {
        return shortsMapper.selectJobById(jobId);
    }

    public void updateJobStarted(Long jobId) {
        shortsMapper.updateJobStarted(jobId);
    }

    public void updateJobFinished(Long jobId, String status, String errorMessage) {
        shortsMapper.updateJobFinished(jobId, status, errorMessage);
    }

    /**
     * 프로젝트에 대한 사용자의 팀 역할 조회.
     * @return "owner", "editor", "viewer" 또는 null(접근 불가)
     */
    public String getTeamRole(Long projectId, Long memberId) {
        return shortsMapper.selectTeamRole(projectId, memberId);
    }
}
