package com.d208.mr_patent_backend.domain.chat.service;

import com.d208.mr_patent_backend.domain.chat.entity.ChatRoom;
import com.d208.mr_patent_backend.domain.chat.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Slf4j
@Service
@RequiredArgsConstructor
public class HeartbeatService {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatRoomRepository chatRoomRepository;

//    private final ConcurrentHashMap<Integer, Instant> heartbeatMap = new ConcurrentHashMap<>();

    public void updateHeartbeat(String roomId, Integer userId){
        ChatRoom chatRoom = chatRoomRepository.findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new RuntimeException("채팅방이 존재하지 않습니다."));
        chatRoom.setHeartbeat(Instant.now());
        chatRoomRepository.save(chatRoom);
        System.out.println("ping" + Instant.now());
    }

    @Scheduled(fixedRate = 2000)
    public void checkUserHeartbeats() {
        List<ChatRoom> activeRooms = chatRoomRepository.findBySessionIdIsNotNull();
        Instant now = Instant.now();

        for (ChatRoom room : activeRooms) {
            Instant lastHeartbeat = room.getHeartbeat();
            if (lastHeartbeat != null && Duration.between(lastHeartbeat, now).getSeconds() > 2) {

                // 상태 메시지 전송
                Map<String, Object> statusMessage = Map.of(
                        "type", "LEAVE",
                        "status", room.getStatus() - 1,
                        "message", "network error"
                );
                messagingTemplate.convertAndSend("/sub/chat/room/" + room.getRoomId(), statusMessage);
                System.out.println(" heartbeat로 네트워크 끊김 감지: " + statusMessage);

                // sessionId 초기화
                room.setSessionId(null);
                // 상태 감소
                room.setStatus(Math.max(0, room.getStatus() - 1));
                chatRoomRepository.save(room);
            }
        }
    }


}
