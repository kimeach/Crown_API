package com.crown.inquiry.service;

import com.crown.inquiry.mapper.InquiryMapper;
import com.crown.member.dto.MemberDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class InquiryService {

    private final InquiryMapper inquiryMapper;
    private final RestTemplate restTemplate;

    @Value("${slack.support.webhook:}")
    private String slackWebhook;

    public void submit(Long memberId, String nickname, String email,
                       String title, String content) {
        inquiryMapper.insert(memberId, title, content);
        sendSlack(nickname, email, title, content);
    }

    public void submitGuest(String email, String title, String content) {
        inquiryMapper.insert(null, title, content);
        sendSlack("(비로그인)", email, title, content);
    }

    private void sendSlack(String nickname, String email, String title, String content) {
        if (slackWebhook == null || slackWebhook.isBlank()) return;
        try {
            String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            String msg = String.format(
                "[고객 문의] %s\n\n닉네임: %s\n이메일: %s\n\n제목: %s\n\n%s",
                time, nickname, email, title, content
            );
            restTemplate.postForEntity(slackWebhook, Map.of("text", msg), String.class);
        } catch (Exception e) {
            log.error("Slack 문의 알림 실패: {}", e.getMessage());
        }
    }
}
