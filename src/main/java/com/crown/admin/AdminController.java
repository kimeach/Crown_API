package com.crown.admin;

import com.crown.common.service.FcmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * 관리자 API (/api/admin/**)
 * 관리자 이메일 체크는 프론트엔드 라우트 가드로 처리
 * (DB/워커 부하 최소화를 위해 별도 Auth 미들웨어 없이 내부망 전용)
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final JdbcTemplate jdbcTemplate;
    private final RestTemplate restTemplate;
    private final FcmService   fcmService;

    @Value("${worker.url}")
    private String workerUrl;

    @Value("${worker.secret}")
    private String workerSecret;

    // ── 대시보드 ──────────────────────────────────────────────────────

    @GetMapping("/dashboard")
    public Map<String, Object> getDashboard(@RequestParam(defaultValue = "7") int days) {

        List<Map<String, Object>> dailyActive = jdbcTemplate.queryForList(
            "SELECT DATE(created_at) AS date, COUNT(DISTINCT member_id) AS dau " +
            "FROM access_log WHERE created_at >= NOW() - INTERVAL ? DAY AND member_id IS NOT NULL " +
            "GROUP BY DATE(created_at) ORDER BY date", days);

        List<Map<String, Object>> dailyRequests = jdbcTemplate.queryForList(
            "SELECT DATE(created_at) AS date, COUNT(*) AS requests " +
            "FROM access_log WHERE created_at >= NOW() - INTERVAL ? DAY " +
            "GROUP BY DATE(created_at) ORDER BY date", days);

        List<Map<String, Object>> dailyErrors = jdbcTemplate.queryForList(
            "SELECT DATE(created_at) AS date, COUNT(*) AS total, " +
            "  SUM(CASE WHEN status_code >= 500 THEN 1 ELSE 0 END) AS errors " +
            "FROM access_log WHERE created_at >= NOW() - INTERVAL ? DAY " +
            "GROUP BY DATE(created_at) ORDER BY date", days);

        Map<String, Object> summary = jdbcTemplate.queryForMap(
            "SELECT " +
            "  (SELECT COUNT(DISTINCT member_id) FROM access_log WHERE created_at >= NOW() - INTERVAL 1 DAY AND member_id IS NOT NULL) AS dau_today, " +
            "  (SELECT COUNT(*) FROM access_log WHERE created_at >= NOW() - INTERVAL 1 DAY) AS requests_today, " +
            "  (SELECT COUNT(*) FROM error_log WHERE created_at >= NOW() - INTERVAL 1 DAY) AS errors_today, " +
            "  (SELECT COUNT(*) FROM member) AS total_members, " +
            "  (SELECT COUNT(*) FROM sm_project) AS total_projects, " +
            "  (SELECT COUNT(*) FROM sm_project WHERE status = 'done') AS done_projects, " +
            "  (SELECT COUNT(*) FROM sm_job WHERE status = 'running') AS running_jobs");

        List<Map<String, Object>> slowApis = jdbcTemplate.queryForList(
            "SELECT path, COUNT(*) AS count, ROUND(AVG(duration_ms)) AS avg_ms, MAX(duration_ms) AS max_ms " +
            "FROM access_log WHERE created_at >= NOW() - INTERVAL ? DAY AND duration_ms IS NOT NULL " +
            "GROUP BY path ORDER BY avg_ms DESC LIMIT 10", days);

        List<Map<String, Object>> recentErrors = jdbcTemplate.queryForList(
            "SELECT log_id, source, level, path, LEFT(message,200) AS message, created_at " +
            "FROM error_log ORDER BY log_id DESC LIMIT 20");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary",       summary);
        result.put("dailyActive",   dailyActive);
        result.put("dailyRequests", dailyRequests);
        result.put("dailyErrors",   dailyErrors);
        result.put("slowApis",      slowApis);
        result.put("recentErrors",  recentErrors);
        return result;
    }

    // ── 사용자 관리 ───────────────────────────────────────────────────

    @GetMapping("/users")
    public List<Map<String, Object>> getUsers(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0")  int offset) {
        return jdbcTemplate.queryForList(
            "SELECT m.member_id, m.nickname, m.email, m.created_at, " +
            "  (SELECT MAX(created_at) FROM access_log WHERE member_id = m.member_id) AS last_active, " +
            "  (SELECT COUNT(*) FROM sm_project WHERE member_id = m.member_id) AS project_count, " +
            "  (SELECT COUNT(*) FROM sm_project WHERE member_id = m.member_id AND status = 'done') AS done_count " +
            "FROM member m ORDER BY m.created_at DESC LIMIT ? OFFSET ?",
            limit, offset);
    }

    @GetMapping("/users/{memberId}")
    public Map<String, Object> getUserDetail(@PathVariable Long memberId) {
        Map<String, Object> user = jdbcTemplate.queryForMap(
            "SELECT m.member_id, m.nickname, m.email, m.profile_img, m.created_at, " +
            "  (SELECT MAX(created_at) FROM access_log WHERE member_id = m.member_id) AS last_active, " +
            "  (SELECT COUNT(*) FROM sm_project WHERE member_id = m.member_id) AS project_count, " +
            "  (SELECT COUNT(*) FROM sm_project WHERE member_id = m.member_id AND status = 'done') AS done_count " +
            "FROM member m WHERE m.member_id = ?", memberId);

        List<Map<String, Object>> projects = jdbcTemplate.queryForList(
            "SELECT p.project_id, p.title, p.status, p.category, p.created_at, " +
            "  p.video_url IS NOT NULL AS has_video, m.nickname AS member_name " +
            "FROM sm_project p JOIN member m ON p.member_id = m.member_id " +
            "WHERE p.member_id = ? ORDER BY p.created_at DESC LIMIT 50",
            memberId);
        user.put("projects", projects);
        return user;
    }

    // ── 프로젝트 관리 ─────────────────────────────────────────────────

    @GetMapping("/projects")
    public List<Map<String, Object>> getProjects(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0")  int offset) {
        String where = status != null ? "WHERE p.status = ?" : "";
        Object[] params = status != null
            ? new Object[]{status, limit, offset}
            : new Object[]{limit, offset};
        return jdbcTemplate.queryForList(
            "SELECT p.project_id, p.title, p.status, p.category, m.nickname AS member_name, " +
            "  p.created_at, p.video_url IS NOT NULL AS has_video " +
            "FROM sm_project p JOIN member m ON p.member_id = m.member_id " +
            where + " ORDER BY p.created_at DESC LIMIT ? OFFSET ?",
            params);
    }

    @DeleteMapping("/projects/{projectId}")
    public Map<String, Object> deleteProject(@PathVariable Long projectId) {
        try {
            // S3 파일 삭제 시도
            HttpHeaders h = new HttpHeaders();
            h.set("X-Worker-Secret", workerSecret);
            restTemplate.exchange(workerUrl + "/storage/project/" + projectId,
                HttpMethod.DELETE, new HttpEntity<>(h), String.class);
        } catch (Exception e) {
            log.warn("Admin S3 삭제 실패 (DB 삭제 계속): {}", e.getMessage());
        }
        jdbcTemplate.update("DELETE FROM sm_job          WHERE project_id = ?", projectId);
        jdbcTemplate.update("DELETE FROM sm_script_history WHERE project_id = ?", projectId);
        jdbcTemplate.update("DELETE FROM sm_project       WHERE project_id = ?", projectId);
        return Map.of("deleted", true, "projectId", projectId);
    }

    // ── 에러 로그 ────────────────────────────────────────────────────

    @GetMapping("/errors")
    public Map<String, Object> getErrors(
            @RequestParam(required = false) String source,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0")  int offset) {
        String where = source != null ? "WHERE source = ?" : "";
        Object[] params = source != null
            ? new Object[]{source, limit, offset}
            : new Object[]{limit, offset};
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT log_id, source, level, path, project_id, LEFT(message,300) AS message, " +
            "  LEFT(stack_trace,500) AS stack_trace, created_at " +
            "FROM error_log " + where + " ORDER BY log_id DESC LIMIT ? OFFSET ?",
            params);
        long total = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM error_log" + (source != null ? " WHERE source=?" : ""),
            Long.class,
            source != null ? new Object[]{source} : new Object[]{});
        return Map.of("rows", rows, "total", total);
    }

    @DeleteMapping("/errors")
    public Map<String, Object> clearErrors(@RequestParam(defaultValue = "7") int days) {
        int deleted = jdbcTemplate.update(
            "DELETE FROM error_log WHERE created_at < NOW() - INTERVAL ? DAY", days);
        return Map.of("deleted", deleted);
    }

    // ── 시스템 상태 ────────────────────────────────────────────────────

    @GetMapping("/system")
    public Map<String, Object> getSystemStatus() {
        Map<String, Object> result = new LinkedHashMap<>();

        // Python 워커 헬스
        try {
            HttpHeaders h = new HttpHeaders();
            h.set("X-Worker-Secret", workerSecret);
            ResponseEntity<Map> resp = restTemplate.exchange(
                workerUrl + "/health", HttpMethod.GET, new HttpEntity<>(h), Map.class);
            result.put("worker", Map.of("status", "ok", "response", resp.getBody()));
        } catch (Exception e) {
            result.put("worker", Map.of("status", "error", "message", e.getMessage()));
        }

        // DB 상태
        try {
            Map<String, Object> dbStats = jdbcTemplate.queryForMap(
                "SELECT " +
                "  (SELECT COUNT(*) FROM member) AS members, " +
                "  (SELECT COUNT(*) FROM sm_project) AS projects, " +
                "  (SELECT COUNT(*) FROM sm_job WHERE status='running') AS running_jobs, " +
                "  (SELECT COUNT(*) FROM error_log WHERE created_at >= NOW() - INTERVAL 1 HOUR) AS errors_1h");
            result.put("db", Map.of("status", "ok", "stats", dbStats));
        } catch (Exception e) {
            result.put("db", Map.of("status", "error", "message", e.getMessage()));
        }

        // Python 워커 에러 로그 최근 5건
        try {
            HttpHeaders h = new HttpHeaders();
            h.set("X-Worker-Secret", workerSecret);
            ResponseEntity<Map> resp = restTemplate.exchange(
                workerUrl + "/admin/error-logs?limit=5",
                HttpMethod.GET, new HttpEntity<>(h), Map.class);
            result.put("worker_errors", resp.getBody() != null ? resp.getBody().get("data") : List.of());
        } catch (Exception e) {
            result.put("worker_errors", List.of());
        }

        return result;
    }

    // ── 공지 발송 ────────────────────────────────────────────────────

    /**
     * 전체 사용자에게 FCM 공지 발송 후 이력 저장
     * POST /api/admin/announce
     * Body: { "title": "...", "message": "..." }
     */
    @PostMapping("/announce")
    public Map<String, Object> sendAnnouncement(@RequestBody Map<String, String> body) {
        String title   = body.getOrDefault("title", "").trim();
        String message = body.getOrDefault("message", "").trim();

        if (title.isEmpty() || message.isEmpty()) {
            throw new IllegalArgumentException("title과 message는 필수입니다.");
        }

        // FCM 토큰이 등록된 회원 조회
        List<String> tokens = jdbcTemplate.queryForList(
            "SELECT fcm_token FROM member WHERE fcm_token IS NOT NULL AND fcm_token != ''",
            String.class);

        int sentCount = fcmService.sendToAll(title, message, tokens);

        // 발송 이력 저장
        jdbcTemplate.update(
            "INSERT INTO announcement (title, message, sent_count) VALUES (?, ?, ?)",
            title, message, tokens.size());

        log.info("[Admin] 공지 발송 완료 — 토큰 {}개 중 {} 성공, 제목: {}", tokens.size(), sentCount, title);
        return Map.of("sent", tokens.size(), "delivered", sentCount);
    }

    /**
     * 공지 발송 이력 조회
     * GET /api/admin/announcements
     */
    @GetMapping("/announcements")
    public List<Map<String, Object>> getAnnouncements() {
        return jdbcTemplate.queryForList(
            "SELECT id, title, LEFT(message, 200) AS message, sent_count, created_at " +
            "FROM announcement ORDER BY id DESC LIMIT 20");
    }
}
