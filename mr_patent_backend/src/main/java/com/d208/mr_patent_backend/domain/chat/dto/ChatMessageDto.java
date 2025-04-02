package com.d208.mr_patent_backend.domain.chat.dto;
import com.d208.mr_patent_backend.domain.chat.entity.ChatMessage;
import lombok.*;

import java.time.LocalDateTime;

// 채팅메세지 dto
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageDto {
    private Integer chatId;            // 채팅 ID
    private String roomId;            // 채팅방 ID
    private Integer userId;            // 보낸 사람 ID
    private Integer receiverId;        // 수신자
    private String message;            // 메시지 내용
    private LocalDateTime timeStamp;   // 보낸 시간
    private boolean read;               // 읽음 여부
    private String messageType;         //메세지 타입
    private String fileUrl;
    private String fileName;


    public static ChatMessageDto fromEntity(ChatMessage entity) {
        return ChatMessageDto.builder()
                .chatId(entity.getChatId())
                .roomId(entity.getRoomId())
                .userId(entity.getUserId())
                .receiverId(entity.getReceiverId())
                .message(entity.getMessage())
                .timeStamp(entity.getTimestamp())
                .read(entity.isRead())
                .messageType(entity.getType())
                .build();
    }
}