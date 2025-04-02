package com.d208.mr_patent_backend.domain.chat.dto;

import lombok.*;
import java.time.LocalDateTime;

// 채팅방 목록 화면에서 보여줄 Dto
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatListDto {
    private String roomId;
    private Integer unreadCount;
    private String lastMessage;
    private Integer receiverId;
    private LocalDateTime lastTimestamp;

}
