package com.d208.mr_patent_backend.domain.chat.controller;

import com.d208.mr_patent_backend.domain.chat.dto.ChatMessageDto;
import com.d208.mr_patent_backend.domain.chat.service.ChatService;
import com.d208.mr_patent_backend.domain.fcm.service.FcmService;
import com.d208.mr_patent_backend.domain.fcm.service.FcmTokenService;
import com.d208.mr_patent_backend.domain.s3.service.S3Service;
import com.d208.mr_patent_backend.domain.user.entity.User;
import com.d208.mr_patent_backend.domain.user.repository.UserRepository;
import com.d208.mr_patent_backend.global.config.firebase.FirebaseConfig;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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

        System.out.println("메세지 전송완료");
        if (message.getFileName() != null && !message.getFileName().isBlank()) {
            String presignedUrl = s3Service.generatePresignedDownloadUrl(message.getFileName());
            message.setFileUrl(presignedUrl);
        }

        ChatMessageDto savedMessage = chatService.saveMessage(message);

        messagingTemplate.convertAndSend("/sub/chat/room/" + message.getRoomId(), savedMessage);

        //fcm 보내기
        if (!message.isRead()) {
            System.out.println("fcm 보낼 준비");
            Integer receiverId = message.getReceiverId();
            String token = fcmTokenService.getTokenByUserId(receiverId);

            Integer userId = message.getUserId();

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("유저 없음"));


            String imageUrl = null;
            if (user.getUserImage() != null && !user.getUserImage().isBlank()) {
                imageUrl = s3Service.generatePresignedDownloadUrl(user.getUserImage());
            }

            if (token != null) {
                Map<String, String> data = new HashMap<>();
                data.put("roomId", message.getRoomId());
                data.put("userId", userId.toString());
                data.put("userName", user.getUserName());
                data.put("userImage", imageUrl);
                data.put("type", "CHAT");

                if (user.getUserRole() == 1) {
                    data.put("expertId", receiverId.toString());
                }

                System.out.println("초기화 여부" + FirebaseConfig.isFirebaseInitialized());
                System.out.println(imageUrl);

                if (FirebaseConfig.isFirebaseInitialized()) {
                    fcmService.sendMessageToToken(
                            token,
                            user.getUserName()+ " 님이 메시지를 보냈습니다!",
                            message.getMessage(),
                            data
                    );
                } else {
                    System.err.println("‼️FCM이 초기화되지 않았습니다. 메시지를 전송할 수 없습니다.");
                }
            }
        }
    }
}
