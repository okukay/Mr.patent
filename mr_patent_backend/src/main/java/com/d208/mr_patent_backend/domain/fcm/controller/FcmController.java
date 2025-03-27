package com.d208.mr_patent_backend.domain.fcm.controller;

import com.d208.mr_patent_backend.domain.fcm.service.FcmService;
import com.d208.mr_patent_backend.domain.fcm.service.FcmTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/fcm")
@RequiredArgsConstructor
public class FcmController {

    private final FcmService fcmService;
    private final FcmTokenService fcmTokenService;

    // 테스트용: userId로 저장된 토큰 조회 → 알림 전송
    @PostMapping("/send/{userId}")
    public String sendTestNotification(@PathVariable Integer userId) {
        String targetToken = fcmTokenService.getTokenByUserId(userId);

        if (targetToken == null) {
            return " FCM 토큰 없음 (userId: " + userId + ")";
        }

        fcmService.sendMessageToToken(
                targetToken,
                "테스트 알림",
                "FCM 전송 성공! userId: " + userId,
                Map.of("type", "TEST", "userId", userId.toString())
        );

        return "✅ FCM 테스트 메시지 전송 완료 (userId: " + userId + ")";
    }
}