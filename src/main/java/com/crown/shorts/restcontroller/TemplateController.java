package com.crown.shorts.restcontroller;

import com.crown.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 공개 템플릿 목록 API (로그인 불필요)
 * GET /api/templates?type=ppt  — 활성 템플릿 목록
 */
@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final JdbcTemplate jdbcTemplate;

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list(
            @RequestParam(defaultValue = "") String type) {
        String sql = "SELECT id, type, name, description, thumbnail_url, config, sort_order FROM sm_template WHERE is_active=1";
        Object[] params;
        if (!type.isBlank()) {
            sql += " AND type=?";
            params = new Object[]{type};
        } else {
            params = new Object[]{};
        }
        sql += " ORDER BY type, sort_order, id";
        return ApiResponse.ok(jdbcTemplate.queryForList(sql, params));
    }
}
