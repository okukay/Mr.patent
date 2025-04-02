package com.d208.mr_patent_backend.domain.chat.dto;

import lombok.*;
import software.amazon.awssdk.services.s3.endpoints.internal.Value;

import java.time.LocalDateTime;

// 채팅방 목록 화면에서 보여줄 Dto
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
// 변리사 이름이랑, 사진 추가 필요

public class ChatListDto {
    private Integer userId;
    private Integer expertId;


    private String roomId;
    private Integer unreadCount;
    private String lastMessage;
    private Integer receiverId;
    private LocalDateTime lastTimestamp;

}
