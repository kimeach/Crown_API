package com.crown.shorts.service;

import com.crown.shorts.dto.ProgressMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProgressService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * /topic/progress/{projectId} 로 진행률 브로드캐스트
     */
    public void send(Long projectId, Long jobId, int percent, String step, String message) {
        ProgressMessage msg = new ProgressMessage(projectId, jobId, percent, step, message);
        String dest = "/topic/progress/" + projectId;
        messagingTemplate.convertAndSend(dest, msg);
        log.debug("[Progress] project={} job={} {}% {} {}", projectId, jobId, percent, step, message);
    }
}
