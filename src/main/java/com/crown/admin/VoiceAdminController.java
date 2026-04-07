package com.crown.admin;

import com.crown.common.dto.ApiResponse;
import com.crown.shorts.service.ShortsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 어드민 목소리 관리 API
 * GET    /api/admin/voices              — 목소리 목록
 * POST   /api/admin/voices/clone        — 목소리 복제 생성
 * DELETE /api/admin/voices/{voiceId}    — 목소리 삭제
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/voices")
@RequiredArgsConstructor
public class VoiceAdminController {

    private final ShortsService shortsService;

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list() {
        return ApiResponse.ok(shortsService.listVoices());
    }

    @PostMapping("/clone")
    public ApiResponse<Map<String, Object>> clone(
            @RequestParam("name") String name,
            @RequestParam(value = "description", defaultValue = "") String description,
            @RequestParam("sample") MultipartFile sample) throws Exception {
        return ApiResponse.ok(shortsService.cloneVoice(
                name, description, sample.getBytes(), sample.getOriginalFilename()));
    }

    @DeleteMapping("/{voiceId}")
    public ApiResponse<?> delete(@PathVariable String voiceId) {
        shortsService.deleteVoice(voiceId);
        return ApiResponse.ok(null);
    }
}
