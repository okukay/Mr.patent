package com.d208.mr_patent_backend.domain.chat.service;

import com.d208.mr_patent_backend.domain.chat.entity.ChatRoom;
import com.d208.mr_patent_backend.domain.chat.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class HeartbeatService {

    private final ChatRoomRepository chatRoomRepository;

    private final ConcurrentHashMap<Integer, Instant> heartbeatMap = new ConcurrentHashMap<>();

    public void updateHeartbeat(String roomId, Integer userId){
        ChatRoom chatRoom = chatRoomRepository.findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new RuntimeException("채팅방이 존재하지 않습니다."));
        chatRoom.setHeartbeat(Instant.now());
        chatRoomRepository.save(chatRoom);
        System.out.println("나 살아있어요" + Instant.now());
    }

    public Instant getLastHeartbeat(Integer userId) {
        return heartbeatMap.get(userId);
    }
    public ConcurrentHashMap<Integer, Instant> getAllHeartbeats() {
        return heartbeatMap;
    }
}
