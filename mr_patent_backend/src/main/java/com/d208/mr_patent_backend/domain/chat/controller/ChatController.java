package com.d208.mr_patent_backend.domain.chat.controller;

import com.d208.mr_patent_backend.domain.chat.dto.ChatMessageDto;
import com.d208.mr_patent_backend.domain.chat.service.ChatService;
import com.d208.mr_patent_backend.domain.fcm.service.FcmService;
import com.d208.mr_patent_backend.domain.fcm.service.FcmTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;
    private final FcmTokenService fcmTokenService;
    private final FcmService fcmService;


    // 클라이언트가 "/pub/chat/message"로 메시지를 보내면 이 메서드가 처리(브로드 캐스트)
    @MessageMapping("/chat/message")
    public void sendMessage(ChatMessageDto message) {
        System.out.println("💬 받은 메시지: " + message.getMessage()); // 로그 찍히는지 확인용
        System.out.println("💬 읽음처리: " + message.isRead()); // 로그 찍히는지 확인용

        //DB 저장
        chatService.saveMessage(message);

        // 특정 구독자들에게 메시지 전송 (구독 주소: /sub/chat/room/{roomId})
        if(message.isRead()){
            messagingTemplate.convertAndSend("/sub/chat/room/" + message.getRoomId(), message);
        }
        //fcm 보내기
        else {
            Integer receiverId = message.getReceiverId();
            String token = fcmTokenService.getTokenByUserId(receiverId);

            if (token != null) {
                fcmService.sendMessageToToken(
                        token,
                        "💬 새 메시지 도착!",
                        message.getMessage(),
                        Map.of(
                                "roomId", message.getRoomId(),
                                "userId", message.getUserId().toString(),
                                "type", "CHAT"
                        )
                );
            }
        }
    }
}
