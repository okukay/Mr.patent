package com.d208.mr_patent_backend.domain.chat.controller;

import com.d208.mr_patent_backend.domain.chat.service.HeartbeatService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@Tag(name = "HeartBeat API", description = "핑퐁 보내기")
@RestController
@RequestMapping("/api/heartbeat")
@RequiredArgsConstructor
public class HeartBeatController임시 {

    private final HeartbeatService heartbeatService;


//   /api/heartbeat{userId}

    @PostMapping("/{roomId}/{userId}")
    public ResponseEntity<Void> updateHeartbeat(@PathVariable String roomId, @PathVariable Integer userId) {
        heartbeatService.updateHeartbeat(roomId, userId);
        return ResponseEntity.ok().build();
    }
}
