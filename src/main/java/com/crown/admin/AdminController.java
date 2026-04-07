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

        // Java API 상태 (자기 자신)
        Runtime rt = Runtime.getRuntime();
        long usedMb  = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
        long totalMb = rt.totalMemory() / 1024 / 1024;
        long maxMb   = rt.maxMemory()   / 1024 / 1024;
        long uptimeSec = java.lang.management.ManagementFactory.getRuntimeMXBean().getUptime() / 1000;
        String uptimeStr = String.format("%dh %dm %ds", uptimeSec/3600, (uptimeSec%3600)/60, uptimeSec%60);
        result.put("java", Map.of(
            "status",    "ok",
            "mem_used",  usedMb  + " MB",
            "mem_total", totalMb + " MB",
            "mem_max",   maxMb   + " MB",
            "uptime",    uptimeStr,
            "java_version", System.getProperty("java.version")
        ));

        // Crown API 최근 에러 5건
        try {
            List<Map<String, Object>> apiErrors = jdbcTemplate.queryForList(
                "SELECT path, message, created_at FROM error_log " +
                "WHERE source='crown_api' ORDER BY created_at DESC LIMIT 5");
            result.put("api_errors", apiErrors);
        } catch (Exception e) {
            result.put("api_errors", List.of());
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

    // ── 기능 로드맵 ───────────────────────────────────────────────────

    @GetMapping("/roadmap")
    public Map<String, Object> getRoadmap(
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "30") int days) {

        String where = days > 0 ? "WHERE proposal_date >= CURDATE() - INTERVAL " + days + " DAY" : "WHERE 1=1";
        if (!status.isBlank()) where += " AND status = '" + status.replace("'", "") + "'";

        List<Map<String, Object>> items = jdbcTemplate.queryForList(
            "SELECT id, proposal_date, feature_name, reference_service, implementation_desc, " +
            "  difficulty, priority, auto_developable, " +
            "  status, branch, created_at, updated_at " +
            "FROM sm_feature_roadmap " + where + " ORDER BY proposal_date DESC, priority ASC");

        Map<String, Object> stats = jdbcTemplate.queryForMap(
            "SELECT " +
            "  COUNT(*) AS total, " +
            "  SUM(status = '검토중')   AS reviewing, " +
            "  SUM(status = '개발중')   AS developing, " +
            "  SUM(status = '개발완료') AS dev_done, " +
            "  SUM(status = '배포완료') AS deployed, " +
            "  SUM(status = '보류')     AS held, " +
            "  SUM(auto_developable = 1) AS auto_count " +
            "FROM sm_feature_roadmap");

        List<Map<String, Object>> monthly = jdbcTemplate.queryForList(
            "SELECT DATE_FORMAT(proposal_date, '%Y-%m') AS month, " +
            "  COUNT(*) AS proposed, " +
            "  SUM(status IN ('배포완료')) AS deployed " +
            "FROM sm_feature_roadmap " +
            "GROUP BY DATE_FORMAT(proposal_date, '%Y-%m') " +
            "ORDER BY month DESC LIMIT 6");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items",   items);
        result.put("stats",   stats);
        result.put("monthly", monthly);
        return result;
    }

    @PostMapping("/roadmap")
    public Map<String, Object> createRoadmapItem(@RequestBody Map<String, Object> body) {
        jdbcTemplate.update(
            "INSERT INTO sm_feature_roadmap " +
            "(proposal_date, feature_name, reference_service, implementation_desc, " +
            " difficulty, estimated_time, priority, auto_developable, auto_dev_reason, status) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, '검토중')",
            body.get("proposal_date"), body.get("feature_name"), body.get("reference_service"),
            body.get("implementation_desc"), body.get("difficulty"), body.get("estimated_time"),
            body.get("priority"), body.getOrDefault("auto_developable", false),
            body.getOrDefault("auto_dev_reason", ""));

        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        return Map.of("id", id, "message", "생성 완료");
    }

    @PatchMapping("/roadmap/{id}/status")
    public Map<String, Object> updateRoadmapStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        String status = String.valueOf(body.get("status"));
        String branch = String.valueOf(body.getOrDefault("branch", ""));
        jdbcTemplate.update(
            "UPDATE sm_feature_roadmap SET status = ?, branch = ?, updated_at = NOW() WHERE id = ?",
            status, branch, id);
        return Map.of("message", "상태 업데이트: " + status);
    }

    @DeleteMapping("/roadmap/{id}")
    public Map<String, Object> deleteRoadmapItem(@PathVariable Long id) {
        jdbcTemplate.update("DELETE FROM sm_feature_roadmap WHERE id = ?", id);
        return Map.of("message", "삭제 완료");
    }

    // 기획 제안 → 기획으로 올리기
    @PostMapping("/roadmap/{id}/promote")
    public Map<String, Object> promoteToPlanning(@PathVariable Long id) {
        Map<String, Object> item = jdbcTemplate.queryForMap(
            "SELECT feature_name, implementation_desc, difficulty, reference_service FROM sm_feature_roadmap WHERE id = ?", id);
        jdbcTemplate.update(
            "INSERT INTO sm_planning (title, description, category, status, source, proposal_id) VALUES (?, ?, ?, '아이디어', 'proposal', ?)",
            item.get("feature_name"), item.get("implementation_desc"), item.get("reference_service"), id);
        jdbcTemplate.update("UPDATE sm_feature_roadmap SET status = '기획이관' WHERE id = ?", id);
        Long planId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        return Map.of("planning_id", planId, "message", "기획으로 이관됨");
    }

    // ── 기획 ────────────────────────────────────────────────────────────

    @GetMapping("/planning")
    public Map<String, Object> getPlanning(
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "") String category) {

        StringBuilder where = new StringBuilder("WHERE 1=1");
        if (!status.isBlank())   where.append(" AND status = '").append(status.replace("'","")).append("'");
        if (!category.isBlank()) where.append(" AND category = '").append(category.replace("'","")).append("'");

        List<Map<String, Object>> items = jdbcTemplate.queryForList(
            "SELECT p.*, " +
            "  (SELECT COUNT(*) FROM sm_dev_task WHERE planning_id = p.id) AS task_total, " +
            "  (SELECT COUNT(*) FROM sm_dev_task WHERE planning_id = p.id AND status = '완료') AS task_done " +
            "FROM sm_planning p " + where + " ORDER BY priority ASC, created_at DESC");

        Map<String, Object> stats = jdbcTemplate.queryForMap(
            "SELECT COUNT(*) AS total, " +
            "  SUM(status='아이디어') AS idea, SUM(status='기획중') AS planning, " +
            "  SUM(status='확정') AS confirmed, SUM(status='진행중') AS in_progress, " +
            "  SUM(status='완료') AS done, SUM(status='보류') AS held " +
            "FROM sm_planning");

        return Map.of("items", items, "stats", stats);
    }

    @PostMapping("/planning")
    public Map<String, Object> createPlanning(@RequestBody Map<String, Object> body) {
        jdbcTemplate.update(
            "INSERT INTO sm_planning (title, description, category, status, priority, target_date, source) " +
            "VALUES (?, ?, ?, ?, ?, ?, 'manual')",
            body.get("title"), body.getOrDefault("description",""), body.getOrDefault("category","기타"),
            body.getOrDefault("status","아이디어"), body.getOrDefault("priority", 3),
            body.get("target_date"));
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        return Map.of("id", id, "message", "기획 생성 완료");
    }

    @PatchMapping("/planning/{id}")
    public Map<String, Object> updatePlanning(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        jdbcTemplate.update(
            "UPDATE sm_planning SET title=COALESCE(?,title), description=COALESCE(?,description), " +
            "  category=COALESCE(?,category), status=COALESCE(?,status), priority=COALESCE(?,priority), " +
            "  target_date=COALESCE(?,target_date), updated_at=NOW() WHERE id=?",
            body.get("title"), body.get("description"), body.get("category"),
            body.get("status"), body.get("priority"), body.get("target_date"), id);
        return Map.of("message", "기획 수정 완료");
    }

    @DeleteMapping("/planning/{id}")
    public Map<String, Object> deletePlanning(@PathVariable Long id) {
        jdbcTemplate.update("DELETE FROM sm_dev_task WHERE planning_id = ?", id);
        jdbcTemplate.update("DELETE FROM sm_planning WHERE id = ?", id);
        return Map.of("message", "삭제 완료");
    }

    // ── 개발 태스크 ──────────────────────────────────────────────────────

    @GetMapping("/planning/{planningId}/tasks")
    public Map<String, Object> getDevTasks(@PathVariable Long planningId) {
        List<Map<String, Object>> tasks = jdbcTemplate.queryForList(
            "SELECT * FROM sm_dev_task WHERE planning_id = ? ORDER BY priority ASC, created_at ASC", planningId);

        Map<String, Object> stats = jdbcTemplate.queryForMap(
            "SELECT COUNT(*) AS total, " +
            "  SUM(status='대기') AS waiting, SUM(status='진행중') AS in_progress, " +
            "  SUM(status='완료') AS done, SUM(status='보류') AS held, " +
            "  SUM(estimated_hours) AS total_hours, SUM(actual_hours) AS actual_hours, " +
            "  SUM(auto_assignable=1) AS auto_count " +
            "FROM sm_dev_task WHERE planning_id = ?", planningId);

        Map<String, Object> planning = jdbcTemplate.queryForMap(
            "SELECT id, title, status, category, target_date FROM sm_planning WHERE id = ?", planningId);

        return Map.of("tasks", tasks, "stats", stats, "planning", planning);
    }

    @PostMapping("/planning/{planningId}/tasks")
    public Map<String, Object> createDevTask(@PathVariable Long planningId, @RequestBody Map<String, Object> body) {
        jdbcTemplate.update(
            "INSERT INTO sm_dev_task (planning_id, title, description, category, status, priority, " +
            "  estimated_hours, auto_assignable, due_date) VALUES (?, ?, ?, ?, '대기', ?, ?, ?, ?)",
            planningId, body.get("title"), body.getOrDefault("description",""),
            body.getOrDefault("category","frontend"), body.getOrDefault("priority", 3),
            body.get("estimated_hours"), body.getOrDefault("auto_assignable", false),
            body.get("due_date"));
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        return Map.of("id", id, "message", "태스크 생성 완료");
    }

    @PatchMapping("/planning/tasks/{taskId}")
    public Map<String, Object> updateDevTask(@PathVariable Long taskId, @RequestBody Map<String, Object> body) {
        String status = (String) body.get("status");
        if ("완료".equals(status)) {
            jdbcTemplate.update(
                "UPDATE sm_dev_task SET status=?, actual_hours=COALESCE(?,actual_hours), completed_at=NOW(), updated_at=NOW() WHERE id=?",
                status, body.get("actual_hours"), taskId);
        } else {
            jdbcTemplate.update(
                "UPDATE sm_dev_task SET title=COALESCE(?,title), description=COALESCE(?,description), " +
                "  category=COALESCE(?,category), status=COALESCE(?,status), priority=COALESCE(?,priority), " +
                "  estimated_hours=COALESCE(?,estimated_hours), auto_assignable=COALESCE(?,auto_assignable), " +
                "  due_date=COALESCE(?,due_date), updated_at=NOW() WHERE id=?",
                body.get("title"), body.get("description"), body.get("category"), body.get("status"),
                body.get("priority"), body.get("estimated_hours"), body.get("auto_assignable"),
                body.get("due_date"), taskId);
        }
        return Map.of("message", "태스크 수정 완료");
    }

    @DeleteMapping("/planning/tasks/{taskId}")
    public Map<String, Object> deleteDevTask(@PathVariable Long taskId) {
        jdbcTemplate.update("DELETE FROM sm_dev_task WHERE id = ?", taskId);
        return Map.of("message", "삭제 완료");
    }
}
