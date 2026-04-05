package com.crown.shorts.dao;

import com.crown.shorts.dto.JobDto;
import com.crown.shorts.dto.ProjectDto;
import com.crown.shorts.dto.QuestionDto;
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

    public void deleteProject(Long projectId) {
        shortsMapper.deleteJobsByProjectId(projectId);
        shortsMapper.deleteProject(projectId);
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
}
