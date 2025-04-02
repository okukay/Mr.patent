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
    private String expertName; //변리사 이름
    private String expertImage; //변리사 사진
}
