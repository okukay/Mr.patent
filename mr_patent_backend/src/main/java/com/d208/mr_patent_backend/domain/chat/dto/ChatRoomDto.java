package com.d208.mr_patent_backend.domain.chat.dto;


import lombok.*;

import java.time.LocalDateTime;

// 채팅방 dto
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoomDto {
    private String roomId;              // 채팅방 ID
    private Integer userId;            // 보낸 사람 ID
    private Integer status;            // 채팅방 상태 ID
    private Integer unreadCount;       // 안읽은 메세지 카운트
    private String sessionId;
    private String lastMessage;                 // 마지막 메세지
    private LocalDateTime lastTimestamp;        // 마지막 보낸시간
    private LocalDateTime created;     // 생성일자 시간
    private LocalDateTime updated;     // 수정일자 여부

}

