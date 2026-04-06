package com.crown.shorts.service;

import com.crown.shorts.dto.JobDto;
import com.crown.shorts.dto.ProjectDto;
import com.crown.shorts.dto.QuestionDto;
import com.crown.shorts.dto.ScriptHistoryDto;

import java.util.List;
import java.util.Map;

public interface ShortsService {

    /** 카테고리별 설문 질문 조회 */
    List<QuestionDto> getQuestions(String category);

    /** 새 프로젝트 생성 후 Python 워커에 데이터 수집 요청 */
    ProjectDto createAndGenerate(Long memberId, String category, String templateId, Map<String, Object> options);

    /** 빈 프로젝트 생성 (워커 호출 없음) */
    ProjectDto createBlank(Long memberId, String outputType);

    /** TTS 미리 듣기 — 오디오 바이트 반환 */
    byte[] getTtsPreview(String text, String voice, String rate);

    /** 내 프로젝트 목록 조회 */
    List<ProjectDto> getMyProjects(Long memberId);

    /** 프로젝트 상세 조회 */
    ProjectDto getProject(Long projectId, Long memberId);

    /** 에디터에서 수정한 대본 저장 */
    void updateScript(Long projectId, Long memberId, Map<String, String> script);

    /** 에디터에서 수정한 HTML 저장 (S3 재업로드) */
    void updateHtml(Long projectId, Long memberId, String html);

    /** 제목 변경 */
    void updateTitle(Long projectId, Long memberId, String title);

    /** 프로젝트 복제 (HTML·대본 복사, 영상 미포함) */
    ProjectDto duplicateProject(Long projectId, Long memberId);

    /** 프로젝트 삭제 (S3 파일 + DB) */
    void deleteProject(Long projectId, Long memberId);

    /** 영상 생성 잡 시작 */
    JobDto startRender(Long projectId, Long memberId, Map<String, Object> renderOptions);

    /** AI 대본 재작성 */
    String rewriteScript(Long projectId, Long memberId, String text, String style, String instruction);

    /** AI 대본 번역 */
    String translateScript(Long projectId, Long memberId, String text, String targetLanguage);

    /** 대본 히스토리 저장 */
    ScriptHistoryDto saveScriptHistory(Long projectId, Long memberId, Map<String, String> script, String note);

    /** 대본 히스토리 목록 조회 */
    List<ScriptHistoryDto> getScriptHistory(Long projectId, Long memberId);

    /** 대본 히스토리로 복원 */
    void restoreScriptHistory(Long projectId, Long memberId, Long historyId);

    /** AI 해시태그 생성 */
    List<String> generateHashtags(Long projectId, Long memberId, String title, String script, int count);

    /** AI SEO 최적화 (제목, 설명, 태그) */
    Map<String, Object> generateSeo(Long projectId, Long memberId, String title, String script);

    /** AI 대본 품질 분석 */
    Map<String, Object> analyzeQuality(Long projectId, Long memberId, String script);

    /** 대본 → SRT 자막 생성 */
    Map<String, Object> generateSubtitleFromScript(Long projectId, Long memberId, Map<String, String> script, String ttsRate);

    /** 영상 → Whisper 자막 생성 */
    Map<String, Object> generateSubtitleFromVideo(Long projectId, Long memberId, String videoUrl, String language);

    /** SRT 자막 다국어 번역 */
    Map<String, Object> translateSubtitle(Long projectId, Long memberId, String srt, String targetLanguage);

    /** 목소리 복제 생성 */
    Map<String, Object> cloneVoice(String name, String description, byte[] sampleBytes, String filename);

    /** 사용 가능한 목소리 목록 */
    List<Map<String, Object>> listVoices();

    /** 목소리 복제 삭제 */
    void deleteVoice(String voiceId);

    /** 클립 미디어(이미지/영상) S3 업로드 — URL 반환 */
    String uploadAsset(Long projectId, Long memberId, byte[] fileBytes, String filename, String contentType);

    /** 잡 상태 조회 */
    JobDto getJobStatus(Long jobId);

    /** PPT 슬라이드 AI 생성 */
    void generatePptSlides(Long projectId, Long memberId, Map<String, Object> options);

    /** HTML → PDF 내보내기 (Python 워커 → S3 → URL 반환) */
    String exportPdf(Long projectId, Long memberId);

    /** HTML → PPTX 내보내기 (Python 워커 → S3 → URL 반환) */
    String exportPptx(Long projectId, Long memberId);

    /** 트렌딩 토픽 조회 */
    List<Map<String, Object>> getTrendingTopics(String category, int limit);

    /** Python 워커 콜백 — 데이터 수집 완료 */
    void onGenerateDone(Long projectId, String htmlUrl, Map<String, String> script, String title, String thumbnailUrl);

    /** Python 워커 콜백 — 데이터 수집 실패 */
    void onGenerateError(Long projectId, String errorMessage);

    /** Python 워커 콜백 — 영상 생성 완료 */
    void onRenderDone(Long jobId, Long projectId, String videoUrl);

    /** Python 워커 콜백 — 영상 생성 실패 */
    void onRenderError(Long jobId, Long projectId, String errorMessage);
}
