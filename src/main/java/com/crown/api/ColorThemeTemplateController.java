package com.crown.api;

import com.google.firebase.auth.FirebaseToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/color-theme-templates")
public class ColorThemeTemplateController {

    private final JdbcTemplate jdbcTemplate;

    // ── 템플릿 조회 (기본 + 로그인 시 사용자 커스텀 포함) ──────────────────
    @GetMapping
    public List<Map<String, Object>> getTemplates(
            @AuthenticationPrincipal FirebaseToken token) {
        String sql;
        List<Object> params = new ArrayList<>();

        if (token != null) {
            // 인증된 사용자: 기본 템플릿 + 본인 커스텀 템플릿
            sql = "SELECT template_id, name, description, layout, color_theme, accent, highlight, bg, text_color, " +
                  "circle1, circle2, font_family, google_fonts_url, is_system, created_by_user_id, is_public, usage_count " +
                  "FROM sm_color_theme_template WHERE is_system = true OR created_by_user_id = ? " +
                  "ORDER BY is_system DESC, template_id";
            params.add(token.getUid());
        } else {
            // 비로그인: 기본 템플릿만
            sql = "SELECT template_id, name, description, layout, color_theme, accent, highlight, bg, text_color, " +
                  "circle1, circle2, font_family, google_fonts_url, is_system, is_public, usage_count " +
                  "FROM sm_color_theme_template WHERE is_system = true ORDER BY template_id";
        }

        try {
            List<Map<String, Object>> templates = jdbcTemplate.queryForList(sql, params.toArray());
            log.info("[ColorThemeTemplate] 조회 완료: {} 개 (uid={})", templates.size(),
                    token != null ? token.getUid() : "anonymous");
            return templates;
        } catch (Exception e) {
            log.error("[ColorThemeTemplate] 조회 실패: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    // ── 특정 템플릿 조회 ────────────────────────────────────────────────────
    @GetMapping("/{templateId}")
    public Map<String, Object> getTemplate(@PathVariable int templateId) {
        String sql = "SELECT template_id, name, description, layout, color_theme, accent, highlight, bg, text_color, " +
                     "circle1, circle2, font_family, google_fonts_url, is_system, is_public, usage_count " +
                     "FROM sm_color_theme_template WHERE template_id = ?";
        try {
            return jdbcTemplate.queryForMap(sql, templateId);
        } catch (Exception e) {
            log.error("[ColorThemeTemplate] 조회 실패: template_id={}", templateId);
            return new HashMap<>();
        }
    }

    // ── 사용자 커스텀 템플릿 저장 ──────────────────────────────────────────
    @PostMapping
    public Map<String, Object> saveTemplate(
            @AuthenticationPrincipal FirebaseToken token,
            @RequestBody TemplateRequest request) {

        if (token == null) {
            return Map.of("success", false, "message", "인증이 필요합니다.");
        }

        // 입력 검증
        if (request.getName() == null || request.getName().isBlank()) {
            return Map.of("success", false, "message", "템플릿 이름은 필수입니다.");
        }
        if (request.getAccent() == null || !request.getAccent().matches("^#[0-9a-fA-F]{6}$")) {
            return Map.of("success", false, "message", "유효하지 않은 색상 코드입니다.");
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
                    token.getUid());

            if (result > 0) {
                log.info("[ColorThemeTemplate] 저장: uid={}, name={}", token.getUid(), request.getName());
                return Map.of("success", true, "message", "템플릿 저장 완료");
            } else {
                return Map.of("success", false, "message", "저장 실패");
            }
        } catch (Exception e) {
            log.error("[ColorThemeTemplate] 저장 실패: uid={}, error={}", token.getUid(), e.getMessage());
            return Map.of("success", false, "message", "서버 오류가 발생했습니다.");
        }
    }

    // ── 템플릿 사용량 증가 (프로젝트 생성 시 호출) ────────────────────────
    @PostMapping("/{templateId}/use")
    public void incrementUsageCount(@PathVariable int templateId) {
        String sql = "UPDATE sm_color_theme_template SET usage_count = usage_count + 1 WHERE template_id = ?";
        try {
            jdbcTemplate.update(sql, templateId);
        } catch (Exception e) {
            log.warn("[ColorThemeTemplate] 사용량 증가 실패: template_id={}", templateId);
        }
    }

    // ── 커스텀 템플릿 삭제 (본인 것만) ──────────────────────────────────────
    @DeleteMapping("/{templateId}")
    public Map<String, Object> deleteTemplate(
            @PathVariable int templateId,
            @AuthenticationPrincipal FirebaseToken token) {

        if (token == null) {
            return Map.of("success", false, "message", "인증이 필요합니다.");
        }

        // 시스템 템플릿 삭제 방지
        String sql = "DELETE FROM sm_color_theme_template WHERE template_id = ? AND created_by_user_id = ? AND is_system = false";
        try {
            int result = jdbcTemplate.update(sql, templateId, token.getUid());
            if (result > 0) {
                log.info("[ColorThemeTemplate] 삭제: template_id={}, uid={}", templateId, token.getUid());
                return Map.of("success", true, "message", "템플릿 삭제 완료");
            } else {
                return Map.of("success", false, "message", "삭제 권한이 없거나 존재하지 않는 템플릿입니다.");
            }
        } catch (Exception e) {
            log.error("[ColorThemeTemplate] 삭제 실패: template_id={}", templateId);
            return Map.of("success", false, "message", "서버 오류가 발생했습니다.");
        }
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
