package com.d208.mr_patent_backend.domain.fcm.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FcmTokenRequestDto {
    private Integer userId;
    private String fcmToken;
}