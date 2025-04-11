package com.d208.mr_patent_backend.domain.chat.controller;

import com.d208.mr_patent_backend.domain.chat.dto.HeartBeatDto;
import com.d208.mr_patent_backend.domain.chat.service.HeartbeatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;


@Slf4j
@Controller
@RequiredArgsConstructor
public class HeartBeatController {

    private final HeartbeatService heartbeatService;

    @MessageMapping("/chat/heartbeat")
    public void receiveHeartbeat(HeartBeatDto heartbeat) {
        Integer userId = heartbeat.getUserId();
        String roomId = heartbeat.getRoomId();

        heartbeatService.updateHeartbeat(roomId, userId);
    }
}
