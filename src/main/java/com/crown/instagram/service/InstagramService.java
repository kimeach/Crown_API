package com.crown.instagram.service;

import com.crown.instagram.dto.InstagramAccountDto;
import java.util.Map;

public interface InstagramService {
    /** 인스타그램 OAuth 인증 URL 생성 */
    String getAuthorizationUrl();

    /** OAuth 콜백 처리 및 토큰 획득/저장 */
    InstagramAccountDto handleOAuthCallback(String code, Long memberId);

    /** 연결된 인스타그램 계정 정보 조회 */
    InstagramAccountDto getConnectedAccount(Long memberId);

    /** 계정 연결 해제 */
    void disconnectAccount(Long memberId);

    /** 액세스 토큰 갱신 (Long-lived token) */
    void refreshAccessToken(Long memberId);
}