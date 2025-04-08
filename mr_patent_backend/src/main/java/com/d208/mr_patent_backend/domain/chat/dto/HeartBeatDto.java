package com.d208.mr_patent_backend.domain.chat.dto;

import lombok.*;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HeartBeatDto {
        private Integer userId;
        private String roomId;
        private String type;  // 보통 "PING" 고정
}
