package com.d208.mr_patent_backend.domain.fcm.controller;

import com.d208.mr_patent_backend.domain.fcm.dto.FcmTokenRequestDto;
import com.d208.mr_patent_backend.domain.fcm.service.FcmTokenService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fcm")
@RequiredArgsConstructor
public class FcmTokenController {

    private final FcmTokenService fcmTokenService;

    @PostMapping("/token")
    public void saveFcmToken(@RequestBody FcmTokenRequestDto request) {
        fcmTokenService.saveOrUpdateToken(request.getUserId(), request.getFcmToken());
    }


}