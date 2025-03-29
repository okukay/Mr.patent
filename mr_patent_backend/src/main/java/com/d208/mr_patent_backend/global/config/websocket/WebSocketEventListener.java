package com.d208.mr_patent_backend.global.config.websocket;
import com.d208.mr_patent_backend.domain.chat.dto.ChatRoomDto;
import com.d208.mr_patent_backend.domain.chat.entity.ChatRoom;
import com.d208.mr_patent_backend.domain.chat.repository.ChatRoomRepository;
import com.d208.mr_patent_backend.domain.chat.service.ChatReadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {


    private final ChatRoomRepository chatRoomRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatReadService chatReadService;

    @EventListener
    public void handleSubscribeEvent(SessionSubscribeEvent event) {
        System.out.println("✅ [SUBSCRIBE 이벤트 발생]");

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());

        String sessionId = accessor.getSessionId();
        String userId = accessor.getFirstNativeHeader("userId");
        String roomId = accessor.getFirstNativeHeader("roomId");

        System.out.println("🧾 [SUBSCRIBE] sessionId = " + sessionId + ", userId = " + userId + ", roomId = " + roomId);

        if (sessionId != null && userId != null && roomId != null) {
            int userid = Integer.parseInt(userId);
            System.out.println("✅ 조건 통과. userid = " + userid);

            chatRoomRepository.findByRoomIdAndUserId(roomId, userid).ifPresent(room -> {
                System.out.println("✅ ChatRoom 찾음: " + room);

                room.setSessionId(sessionId);  // 1. sessionId 저장
                room.setStatus(room.getStatus() + 1); // 2. status 증가
                chatRoomRepository.save(room);
                System.out.println("💾 ChatRoom 저장 완료");

                chatReadService.handleUserEntered(roomId, userid);

                int totalStatus = chatRoomRepository.findByRoomId(roomId).stream()
                        .mapToInt(ChatRoom::getStatus)
                        .sum();

                Map<String, Object> statusMessage = Map.of(
                        "type", "ENTER",
                        "status", totalStatus,
                        "enteredUserId", userId
                );
                // 3. 상태 전송
                messagingTemplate.convertAndSend("/sub/chat/room/" + roomId, statusMessage);
                System.out.println("📤 상태 메시지 전송 완료: " + statusMessage);
            });
        }
    }
    @EventListener
    public void handleDisconnectEvent(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        System.out.println("❌ [DISCONNECT 이벤트 발생] sessionId = " + sessionId);

        chatRoomRepository.findBySessionId(sessionId).ifPresentOrElse(room -> {
            System.out.println("🧾 [DISCONNECT 처리] roomId = " + room.getRoomId() + ", userId = " + room.getUserId());

            String roomId = room.getRoomId();
            // 1. status -1 처리
            int updatedStatus = Math.max(0, room.getStatus() - 1);
            room.setStatus(updatedStatus);

            chatRoomRepository.save(room);
            System.out.println("💾 status 업데이트 완료: " + updatedStatus);

            int totalStatus = chatRoomRepository.findByRoomId(roomId).stream()
                    .mapToInt(ChatRoom::getStatus)
                    .sum();

            Map<String, Object> statusMessage = Map.of(
                    "type", "LEAVE",
                    "status", totalStatus
            );
            // 2. STOMP 상태 전송
            messagingTemplate.convertAndSend("/sub/chat/room/" + room.getRoomId(),statusMessage);
            System.out.println("📤 상태 메시지 전송 완료" + statusMessage);

        }, () -> {
            System.out.println("❗ [DISCONNECT 처리 실패] sessionId로 채팅방을 찾을 수 없습니다: " + sessionId);
        });
    }
}

