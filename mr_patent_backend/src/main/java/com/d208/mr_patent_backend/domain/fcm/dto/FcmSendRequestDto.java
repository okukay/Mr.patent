package com.d208.mr_patent_backend.domain.fcm.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class FcmSendRequestDto {
    private String title;
    private String body;
    private Map<String, String> data;
}
