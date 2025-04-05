package com.d208.mr_patent_backend.domain.chat.controller;

import com.d208.mr_patent_backend.domain.chat.dto.ChatMessageDto;
import com.d208.mr_patent_backend.domain.chat.service.ChatService;
import com.d208.mr_patent_backend.domain.fcm.service.FcmService;
import com.d208.mr_patent_backend.domain.fcm.service.FcmTokenService;
import com.d208.mr_patent_backend.domain.s3.service.S3Service;
import com.d208.mr_patent_backend.domain.user.entity.User;
import com.d208.mr_patent_backend.domain.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "채팅 API", description = "채팅 보내기")
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;
    private final FcmTokenService fcmTokenService;
    private final FcmService fcmService;
    private final UserRepository userRepository;
    private final S3Service s3Service;


    // 클라이언트가 "/pub/chat/message"로 메시지를 보내면 이 메서드가 처리(브로드 캐스트)
    @MessageMapping("/chat/message")
    public void sendMessage(ChatMessageDto message) {

        if (message.getFileName() != null && !message.getFileName().isBlank()) {
            String presignedUrl = s3Service.generatePresignedDownloadUrl(message.getFileName());
            message.setFileUrl(presignedUrl);
        }

        //DB 저장
        ChatMessageDto savedMessage = chatService.saveMessage(message);

        // 웹소켓 전송
        messagingTemplate.convertAndSend("/sub/chat/room/" + message.getRoomId(), savedMessage);


        //fcm 보내기
        if(!message.isRead()) {
            System.out.println("fcm 보낼 준비");
            Integer receiverId = message.getReceiverId();
            String token = fcmTokenService.getTokenByUserId(receiverId);

            Integer userId = message.getUserId();

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("유저 없음"));

            if (token != null) {
                Map<String, String> data = new HashMap<>();
                data.put("roomId", message.getRoomId());
                data.put("userId", userId.toString());
                data.put("userName", user.getUserName());
                data.put("userImage", user.getUserImage());
                data.put("type", "CHAT");

                if (user.getUserRole() == 1) {
                    data.put("expertId", receiverId.toString());
                }

                fcmService.sendMessageToToken(
                        token,
                        "새 메시지 도착!",
                        message.getMessage(),
                        data
                );
                System.out.println("fcm 이 보내졋다.");
            }
        }
    }
}
