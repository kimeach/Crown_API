package com.crown.admin;

import com.crown.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.sql.PreparedStatement;
import java.util.*;

/**
 * 어드민 효과음/배경음악 관리 API
 * POST /api/admin/sounds/sfx/upload  — 효과음 업로드 (Python 워커 → S3)
 * GET  /api/admin/sounds/sfx         — 효과음 목록
 * PUT  /api/admin/sounds/sfx/{id}    — 이름/태그 수정
 * DELETE /api/admin/sounds/sfx/{id}  — 삭제
 * (BGM도 동일 구조)
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/sounds")
@RequiredArgsConstructor
public class SoundsAdminController {

    private final JdbcTemplate jdbcTemplate;
    private final RestTemplate restTemplate;

    @Value("${worker.url}")
    private String workerUrl;

    @Value("${worker.secret}")
    private String workerSecret;

    // ══ SFX ══════════════════════════════════════════════════════

    @PostMapping("/sfx/upload")
    public ResponseEntity<?> uploadSfx(@RequestParam("file") MultipartFile file) {
        return handleUpload(file, "sfx");
    }

    @PostMapping("/sfx/batch")
    public ResponseEntity<?> batchUploadSfx(@RequestParam("files") List<MultipartFile> files) {
        return handleBatchUpload(files, "sfx");
    }

    @GetMapping("/sfx")
    public ApiResponse<List<Map<String, Object>>> listSfx(
            @RequestParam(defaultValue = "") String q) {
        return ApiResponse.ok(listSounds("sfx", q));
    }

    @PutMapping("/sfx/{id}")
    public ApiResponse<?> updateSfx(@PathVariable int id, @RequestBody Map<String, Object> body) {
        updateSound("sfx", id, body);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/sfx/{id}")
    public ApiResponse<?> deleteSfx(@PathVariable int id) {
        deleteSound("sfx", id);
        return ApiResponse.ok(null);
    }

    // ══ BGM ══════════════════════════════════════════════════════

    @PostMapping("/bgm/upload")
    public ResponseEntity<?> uploadBgm(@RequestParam("file") MultipartFile file) {
        return handleUpload(file, "bgm");
    }

    @PostMapping("/bgm/batch")
    public ResponseEntity<?> batchUploadBgm(@RequestParam("files") List<MultipartFile> files) {
        return handleBatchUpload(files, "bgm");
    }

    @GetMapping("/bgm")
    public ApiResponse<List<Map<String, Object>>> listBgm(
            @RequestParam(defaultValue = "") String q) {
        return ApiResponse.ok(listSounds("bgm", q));
    }

    @PutMapping("/bgm/{id}")
    public ApiResponse<?> updateBgm(@PathVariable int id, @RequestBody Map<String, Object> body) {
        updateSound("bgm", id, body);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/bgm/{id}")
    public ApiResponse<?> deleteBgm(@PathVariable int id) {
        deleteSound("bgm", id);
        return ApiResponse.ok(null);
    }

    // ══ 공통 로직 ══════════════════════════════════════════════════

    @SuppressWarnings("unchecked")
    private ResponseEntity<?> handleBatchUpload(List<MultipartFile> files, String type) {
        List<Map<String, Object>> results = new ArrayList<>();
        for (MultipartFile file : files) {
            try {
                ResponseEntity<?> resp = handleUpload(file, type);
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("ok", true);
                row.put("filename", file.getOriginalFilename());
                // handleUpload returns ResponseEntity<ApiResponse<Map>> — unwrap
                Object body = resp.getBody();
                if (body instanceof ApiResponse) {
                    Object data = ((ApiResponse<?>) body).getData();
                    if (data instanceof Map) row.putAll((Map<String, Object>) data);
                }
                results.add(row);
            } catch (Exception e) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("ok", false);
                row.put("filename", file.getOriginalFilename());
                row.put("error", e.getMessage());
                results.add(row);
            }
        }
        return ResponseEntity.ok(ApiResponse.ok(results));
    }

    private ResponseEntity<?> handleUpload(MultipartFile file, String type) {
        try {
            // 1. Python 워커에 파일 전달
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.set("X-Worker-Secret", workerSecret);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
                @Override public String getFilename() { return file.getOriginalFilename(); }
            };
            body.add("file", new HttpEntity<>(resource, new HttpHeaders()));

            ResponseEntity<Map> resp = restTemplate.exchange(
                workerUrl + "/sounds/" + type + "/upload",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                Map.class
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) resp.getBody();
            if (result == null) return ResponseEntity.status(500).body("워커 응답 없음");

            // 2. DB 저장
            String name     = (String) result.get("name");
            String tagsJson = tagsToJson(result.get("tags"));
            double duration = toDouble(result.get("duration"));
            String s3Url    = (String) result.get("s3_url");
            String source   = result.containsKey("source") ? (String) result.get("source") : "upload";

            GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(con -> {
                PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO sm_" + type + " (name, tags, duration, s3_url, source) VALUES (?,?,?,?,?)",
                    new String[]{"id"}
                );
                ps.setString(1, name);
                ps.setString(2, tagsJson);
                ps.setDouble(3, duration);
                ps.setString(4, s3Url);
                ps.setString(5, source);
                return ps;
            }, keyHolder);

            Map<String, Object> out = new LinkedHashMap<>(result);
            out.put("id", keyHolder.getKey());
            return ResponseEntity.ok(ApiResponse.ok(out));

        } catch (Exception e) {
            log.error("Sound upload failed", e);
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    private List<Map<String, Object>> listSounds(String type, String q) {
        String sql = "SELECT id, name, tags, duration, s3_url, source, created_at FROM sm_" + type;
        Object[] params;
        if (q != null && !q.isBlank()) {
            sql += " WHERE name LIKE ? OR tags LIKE ?";
            params = new Object[]{"%" + q + "%", "%" + q + "%"};
        } else {
            params = new Object[]{};
        }
        sql += " ORDER BY id DESC";
        return jdbcTemplate.queryForList(sql, params);
    }

    private void updateSound(String type, int id, Map<String, Object> body) {
        String name     = (String) body.get("name");
        String tagsJson = tagsToJson(body.get("tags"));
        jdbcTemplate.update(
            "UPDATE sm_" + type + " SET name=?, tags=? WHERE id=?",
            name, tagsJson, id
        );
    }

    private void deleteSound(String type, int id) {
        jdbcTemplate.update("DELETE FROM sm_" + type + " WHERE id=?", id);
    }

    @SuppressWarnings("unchecked")
    private String tagsToJson(Object tags) {
        if (tags == null) return "[]";
        if (tags instanceof List) {
            StringBuilder sb = new StringBuilder("[");
            List<String> list = (List<String>) tags;
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append("\"").append(list.get(i).replace("\"", "\\\"")).append("\"");
            }
            sb.append("]");
            return sb.toString();
        }
        return tags.toString();
    }

    private double toDouble(Object val) {
        if (val == null) return 0.0;
        if (val instanceof Number) return ((Number) val).doubleValue();
        try { return Double.parseDouble(val.toString()); } catch (Exception e) { return 0.0; }
    }
}
