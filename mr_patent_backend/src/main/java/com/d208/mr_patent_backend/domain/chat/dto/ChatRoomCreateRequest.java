package com.d208.mr_patent_backend.domain.chat.dto;

import lombok.*;

// 채팅방 생성시 필요한 Dto
@Data
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoomCreateRequest {
    private Integer userId;
    private Integer receiverId;
}
