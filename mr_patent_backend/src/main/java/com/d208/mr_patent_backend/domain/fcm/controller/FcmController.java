package com.d208.mr_patent_backend.domain.fcm.controller;

import com.d208.mr_patent_backend.domain.fcm.dto.FcmSendRequestDto;
import com.d208.mr_patent_backend.domain.fcm.service.FcmService;
import com.d208.mr_patent_backend.domain.fcm.service.FcmTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/fcm")
@RequiredArgsConstructor

//@AuthenticationPrincipal CustomUserDetails userDetails
public class FcmController {


    private final FcmTokenService fcmTokenService;

    @DeleteMapping("/token/delete/{userId}")
    public ResponseEntity<Void> deleteFcmToken(@PathVariable Integer userId) {
        fcmTokenService.deleteFcmToken(userId);
        return ResponseEntity.ok().build();
    }
}