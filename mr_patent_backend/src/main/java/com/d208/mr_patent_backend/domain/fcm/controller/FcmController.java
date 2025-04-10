package com.d208.mr_patent_backend.domain.fcm.controller;

import com.d208.mr_patent_backend.domain.fcm.dto.FcmFromPythonDto;
import com.d208.mr_patent_backend.domain.fcm.entity.FcmToken;
import com.d208.mr_patent_backend.domain.fcm.repository.FcmTokenRepository;
import com.d208.mr_patent_backend.domain.fcm.service.FcmService;
import com.d208.mr_patent_backend.domain.fcm.service.FcmTokenService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/fcm")
@RequiredArgsConstructor
public class FcmController {

    private final FcmTokenService fcmTokenService;
    private final FcmService fcmService;
    private final FcmTokenRepository fcmTokenRepository;

    @Operation(summary = "FastAPI 에서 FCM 요청")
    @PostMapping("/token/python")
    public ResponseEntity<String> sendFcmFromPython(@RequestBody FcmFromPythonDto request) {

        Integer userId = request.getUserId();
        String title = request.getTitle();
        String body = request.getBody();
        String dataLog = request.getData() != null ? request.getData().toString() : "null";

        System.out.println("\n🔔 [FCM 요청 수신 - FastAPI]");
        System.out.println("📌 userId: " + userId);
        System.out.println("📝 title: " + title);
        System.out.println("📝 body: " + body);
        System.out.println("📦 data: " + dataLog);

        // 🔍 토큰 조회
        String targetToken = fcmTokenRepository.findByUserId(userId)
                .map(FcmToken::getToken)
                .orElseThrow(() -> new RuntimeException("해당 유저의 FCM 토큰이 존재하지 않습니다."));

        System.out.println("토큰 : " + targetToken);


        // 💬 Map<String, Object> → Map<String, String> 변환
        Map<String, String> stringDataMap = new HashMap<>();
        if (request.getData() != null) {
            request.getData().forEach((key, value) -> {
                stringDataMap.put(key, value != null ? value.toString() : "null");
            });
        }

        fcmService.sendMessageToToken(
                targetToken,
                title,
                body,
                stringDataMap
        );
        return ResponseEntity.ok("✅ FCM 토큰 발송 완료");
    }

    @Operation(summary = "FCM 토큰 삭제")
    @DeleteMapping("/token/delete/{userId}")
    public ResponseEntity<Void> deleteFcmToken(@PathVariable Integer userId) {
        fcmTokenService.deleteFcmToken(userId);
        return ResponseEntity.ok().build();
    }
}