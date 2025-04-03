package com.d208.mr_patent_backend.domain.fcm.controller;

import com.d208.mr_patent_backend.domain.fcm.dto.FcmTokenRequestDto;
import com.d208.mr_patent_backend.domain.fcm.service.FcmTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "FCM API", description = "FCM 저장")

@RestController
@RequestMapping("/api/fcm")
@RequiredArgsConstructor
public class FcmTokenController {

    private final FcmTokenService fcmTokenService;

    @Operation(summary = "FCM 토큰 저장")
    @PostMapping("/token")
    public ResponseEntity<String> saveFcmToken(@RequestBody FcmTokenRequestDto request) {
        fcmTokenService.saveOrUpdateToken(request.getUserId(), request.getFcmToken());
        return ResponseEntity.ok("FCM 토큰 저장 완료");
    }



}