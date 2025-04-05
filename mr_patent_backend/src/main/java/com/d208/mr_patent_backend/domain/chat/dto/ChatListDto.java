package com.d208.mr_patent_backend.domain.chat.dto;

import lombok.*;

import java.time.Instant;
import java.time.LocalDateTime;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

// 채팅방 목록 화면에서 보여줄 Dto
public class ChatListDto {
    private Integer userId;
    private Integer expertId;
    private String roomId;
    private Integer unreadCount;
    private String lastMessage;
    private Instant lastTimestamp;
    private String userName;
    private String userImage;
    private Integer receiverId;

}
