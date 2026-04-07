package com.crown.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/color-theme-templates")
public class ColorThemeTemplateController {

    private final JdbcTemplate jdbcTemplate;

    // ── 템플릿 조회 (기본 + 사용자 커스텀) ────────────────────────────────────
    @GetMapping
    public List<Map<String, Object>> getTemplates(@RequestParam(required = false) String userId) {
        String sql;
        List<Object> params = new ArrayList<>();

        if (userId != null && !userId.isEmpty()) {
            // 기본 템플릿 + 해당 사용자의 커스텀 템플릿
            sql = "SELECT * FROM sm_color_theme_template WHERE is_system = true OR created_by_user_id = ? ORDER BY is_system DESC, template_id";
            params.add(userId);
        } else {
            // 기본 템플릿만
            sql = "SELECT * FROM sm_color_theme_template WHERE is_system = true ORDER BY template_id";
        }

        List<Map<String, Object>> templates = new ArrayList<>();
        try {
            templates = jdbcTemplate.queryForList(sql, params.toArray());
            log.info("[Template] 조회 완료: {} 개 (userId={})", templates.size(), userId);
        } catch (Exception e) {
            log.error("[Template] 조회 실패: {}", e.getMessage());
        }
        return templates;
    }

    // ── 특정 템플릿 조회 ────────────────────────────────────────────────────
    @GetMapping("/{templateId}")
    public Map<String, Object> getTemplate(@PathVariable int templateId) {
        String sql = "SELECT * FROM sm_color_theme_template WHERE template_id = ?";
        try {
            Map<String, Object> template = jdbcTemplate.queryForMap(sql, templateId);
            log.info("[Template] 조회 완료: template_id={}", templateId);
            return template;
        } catch (Exception e) {
            log.error("[Template] 조회 실패: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    // ── 사용자 커스텀 템플릿 저장 ──────────────────────────────────────────
    @PostMapping
    public Map<String, Object> saveTemplate(
            @RequestHeader("Authorization") String authorization,
            @RequestBody TemplateRequest request) {

        String userId = extractUserIdFromToken(authorization);
        if (userId == null || userId.isEmpty()) {
            return Map.of("success", false, "message", "인증 필요");
        }

        String sql = "INSERT INTO sm_color_theme_template " +
                "(name, layout, color_theme, accent, highlight, bg, text_color, circle1, circle2, " +
                "font_family, google_fonts_url, is_system, created_by_user_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, false, ?)";

        try {
            int result = jdbcTemplate.update(sql,
                    request.getName(),
                    request.getLayout(),
                    request.getColorTheme(),
                    request.getAccent(),
                    request.getHighlight(),
                    request.getBg(),
                    request.getTextColor(),
                    request.getCircle1(),
                    request.getCircle2(),
                    request.getFontFamily(),
                    request.getGoogleFontsUrl(),
                    userId);

            if (result > 0) {
                log.info("[Template] 커스텀 템플릿 저장: user_id={}, name={}", userId, request.getName());
                return Map.of("success", true, "message", "템플릿 저장 완료");
            } else {
                return Map.of("success", false, "message", "저장 실패");
            }
        } catch (Exception e) {
            log.error("[Template] 저장 실패: {}", e.getMessage());
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    // ── 템플릿 사용량 증가 (프로젝트 생성 시 호출) ────────────────────────
    @PostMapping("/{templateId}/use")
    public void incrementUsageCount(@PathVariable int templateId) {
        String sql = "UPDATE sm_color_theme_template SET usage_count = usage_count + 1 WHERE template_id = ?";
        try {
            jdbcTemplate.update(sql, templateId);
        } catch (Exception e) {
            log.warn("[Template] 사용량 증가 실패: template_id={}", templateId);
        }
    }

    // ── 커스텀 템플릿 삭제 ────────────────────────────────────────────────
    @DeleteMapping("/{templateId}")
    public Map<String, Object> deleteTemplate(
            @PathVariable int templateId,
            @RequestHeader("Authorization") String authorization) {

        String userId = extractUserIdFromToken(authorization);

        String sql = "DELETE FROM sm_color_theme_template WHERE template_id = ? AND created_by_user_id = ?";
        try {
            int result = jdbcTemplate.update(sql, templateId, userId);
            if (result > 0) {
                log.info("[Template] 삭제 완료: template_id={}, user_id={}", templateId, userId);
                return Map.of("success", true, "message", "템플릿 삭제 완료");
            } else {
                return Map.of("success", false, "message", "해당 템플릿을 삭제할 권한 없음");
            }
        } catch (Exception e) {
            log.error("[Template] 삭제 실패: {}", e.getMessage());
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    // ── 헬퍼: Authorization 헤더에서 user ID 추출 ──────────────────────
    private String extractUserIdFromToken(String authHeader) {
        // Firebase ID 토큰에서 user ID 추출 (실제로는 JWT 디코딩 필요)
        // 현재는 Authorization 헤더 값 그대로 사용 (나중에 개선)
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    // ── DTO ────────────────────────────────────────────────────────────
    @lombok.Data
    @lombok.NoArgsConstructor
    public static class TemplateRequest {
        private String name;
        private String layout;
        private String colorTheme;
        private String accent;
        private String highlight;
        private String bg;
        private String textColor;
        private String circle1;
        private String circle2;
        private String fontFamily;
        private String googleFontsUrl;
    }
}
