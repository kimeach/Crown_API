package com.crown.admin;

import com.crown.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.web.bind.annotation.*;

import java.sql.PreparedStatement;
import java.util.*;

/**
 * 어드민 템플릿 관리 API
 * GET    /api/admin/templates          — 전체 목록 (type 필터 가능)
 * POST   /api/admin/templates          — 신규 생성
 * PUT    /api/admin/templates/{id}     — 수정
 * DELETE /api/admin/templates/{id}     — 삭제
 * PATCH  /api/admin/templates/{id}/toggle — 활성/비활성 토글
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/templates")
@RequiredArgsConstructor
public class TemplateAdminController {

    private final JdbcTemplate jdbcTemplate;

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list(
            @RequestParam(defaultValue = "") String type) {
        String sql = "SELECT id, type, name, description, thumbnail_url, config, is_active, sort_order, created_at FROM sm_template";
        Object[] params;
        if (!type.isBlank()) {
            sql += " WHERE type = ?";
            params = new Object[]{type};
        } else {
            params = new Object[]{};
        }
        sql += " ORDER BY type, sort_order, id";
        return ApiResponse.ok(jdbcTemplate.queryForList(sql, params));
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        String type        = str(body, "type", "video");
        String name        = str(body, "name", "");
        String description = str(body, "description", "");
        String thumbUrl    = str(body, "thumbnail_url", null);
        String config      = str(body, "config", "{}");
        int sortOrder      = body.containsKey("sort_order") ? ((Number) body.get("sort_order")).intValue() : 0;

        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                "INSERT INTO sm_template (type, name, description, thumbnail_url, config, sort_order) VALUES (?,?,?,?,?,?)",
                new String[]{"id"}
            );
            ps.setString(1, type);
            ps.setString(2, name);
            ps.setString(3, description);
            ps.setString(4, thumbUrl);
            ps.setString(5, config);
            ps.setInt(6, sortOrder);
            return ps;
        }, keyHolder);

        Map<String, Object> out = new LinkedHashMap<>(body);
        out.put("id", keyHolder.getKey());
        return ApiResponse.ok(out);
    }

    @PutMapping("/{id}")
    public ApiResponse<?> update(@PathVariable int id, @RequestBody Map<String, Object> body) {
        jdbcTemplate.update(
            "UPDATE sm_template SET name=?, description=?, thumbnail_url=?, config=?, sort_order=? WHERE id=?",
            str(body, "name", ""),
            str(body, "description", ""),
            str(body, "thumbnail_url", null),
            str(body, "config", "{}"),
            body.containsKey("sort_order") ? ((Number) body.get("sort_order")).intValue() : 0,
            id
        );
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<?> delete(@PathVariable int id) {
        jdbcTemplate.update("DELETE FROM sm_template WHERE id=?", id);
        return ApiResponse.ok(null);
    }

    @PatchMapping("/{id}/toggle")
    public ApiResponse<?> toggle(@PathVariable int id) {
        jdbcTemplate.update("UPDATE sm_template SET is_active = 1 - is_active WHERE id=?", id);
        return ApiResponse.ok(null);
    }

    private String str(Map<String, Object> m, String key, String def) {
        Object v = m.get(key);
        if (v == null) return def;
        String s = v.toString().trim();
        return s.isEmpty() ? def : s;
    }
}
