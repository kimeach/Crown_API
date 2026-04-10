package com.crown.instagram.serviceimpl;

import com.crown.instagram.dao.InstagramDao;
import com.crown.instagram.dto.InstagramAccountDto;
import com.crown.instagram.service.InstagramService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class InstagramServiceImpl implements InstagramService {

    private final InstagramDao instagramDao;
    private final RestTemplate restTemplate;

    @Value("${instagram.client-id:}")
    private String clientId;

    @Value("${instagram.client-secret:}")
    private String clientSecret;

    @Value("${instagram.redirect-uri:}")
    private String redirectUri;

    private static final String AUTH_URL = "https://api.instagram.com/oauth/authorize";
    private static final String TOKEN_URL = "https://api.instagram.com/oauth/access_token";
    private static final String GRAPH_URL = "https://graph.instagram.com";

    @Override
    public String getAuthorizationUrl() {
        return UriComponentsBuilder.fromHttpUrl(AUTH_URL)
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("scope", "user_profile,user_media")
                .queryParam("response_type", "code")
                .build().toUriString();
    }

    @Override
    public InstagramAccountDto handleOAuthCallback(String code, Long memberId) {
        // 1. Exchange code for short-lived access token
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("grant_type", "authorization_code");
        params.add("redirect_uri", redirectUri);
        params.add("code", code);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(TOKEN_URL, request, Map.class);

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            Map<String, Object> body = response.getBody();
            String shortAccessToken = (String) body.get("access_token");
            String instagramUserId = String.valueOf(body.get("user_id"));

            // 2. Exchange for long-lived token (60 days)
            String longLivedTokenUrl = UriComponentsBuilder.fromHttpUrl(GRAPH_URL + "/access_token")
                    .queryParam("grant_type", "ig_exchange_token")
                    .queryParam("client_secret", clientSecret)
                    .queryParam("access_token", shortAccessToken)
                    .build().toUriString();

            ResponseEntity<Map> longLivedResponse = restTemplate.getForEntity(longLivedTokenUrl, Map.class);
            
            if (longLivedResponse.getStatusCode() == HttpStatus.OK && longLivedResponse.getBody() != null) {
                Map<String, Object> llBody = longLivedResponse.getBody();
                
                // 3. Get User Profile (Username)
                String profileUrl = UriComponentsBuilder.fromHttpUrl(GRAPH_URL + "/me")
                        .queryParam("fields", "id,username")
                        .queryParam("access_token", (String) llBody.get("access_token"))
                        .build().toUriString();
                ResponseEntity<Map> profileResponse = restTemplate.getForEntity(profileUrl, Map.class);

                InstagramAccountDto account = new InstagramAccountDto();
                account.setMemberId(memberId);
                account.setInstagramUserId(instagramUserId);
                account.setAccessToken((String) llBody.get("access_token"));
                account.setTokenType((String) llBody.get("token_type"));
                account.setExpiresIn(Long.valueOf(String.valueOf(llBody.get("expires_in"))));
                
                if (profileResponse.getBody() != null) {
                    account.setUsername((String) profileResponse.getBody().get("username"));
                }

                instagramDao.saveAccount(account);
                return account;
            }
        }
        throw new RuntimeException("Instagram authentication failed");
    }

    @Override
    public InstagramAccountDto getConnectedAccount(Long memberId) {
        return instagramDao.getAccountByMemberId(memberId);
    }

    @Override
    public void disconnectAccount(Long memberId) {
        instagramDao.removeAccount(memberId);
    }

    @Override
    public void refreshAccessToken(Long memberId) {
        InstagramAccountDto account = instagramDao.getAccountByMemberId(memberId);
        if (account == null) return;

        String refreshUrl = UriComponentsBuilder.fromHttpUrl(GRAPH_URL + "/refresh_access_token")
                .queryParam("grant_type", "ig_refresh_token")
                .queryParam("access_token", account.getAccessToken())
                .build().toUriString();

        ResponseEntity<Map> response = restTemplate.getForEntity(refreshUrl, Map.class);
        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            Map<String, Object> body = response.getBody();
            account.setAccessToken((String) body.get("access_token"));
            account.setExpiresIn(Long.valueOf(String.valueOf(body.get("expires_in"))));
            instagramDao.saveAccount(account);
        }
    }
}