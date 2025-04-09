package com.d208.mr_patent_backend.domain.fcm.controller;

import com.d208.mr_patent_backend.domain.fcm.dto.FcmFromPythonDto;
import com.d208.mr_patent_backend.domain.fcm.dto.FcmSendRequestDto;
import com.d208.mr_patent_backend.domain.fcm.dto.FcmTokenRequestDto;
import com.d208.mr_patent_backend.domain.fcm.entity.FcmToken;
import com.d208.mr_patent_backend.domain.fcm.repository.FcmTokenRepository;
import com.d208.mr_patent_backend.domain.fcm.service.FcmService;
import com.d208.mr_patent_backend.domain.fcm.service.FcmTokenService;
import io.swagger.v3.oas.annotations.Operation;
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
    private final FcmService fcmService;
    private final FcmTokenRepository fcmTokenRepository;

    @Operation(summary = "파이썬 에서 FCM 요청")
    @PostMapping("/token/python")
    public ResponseEntity<String> sendFcmFromPython(@RequestBody FcmFromPythonDto request) {
        String userId = request.getUserId();
        FcmToken fcmToken = fcmTokenRepository.findByUserId(userId);
        String targetToken = fcmToken.getToken();
        fcmService.sendMessageToToken(
                targetToken,
                request.getTitle(),
                request.getBody(),
                request.getData()
        );
        return ResponseEntity.ok("FCM 토큰 발송 완료");
    }

//    @PostMapping("/send")
//    public ResponseEntity<?> sendNotification(@RequestBody NotificationRequest request) {
//        try {
//            String messageId = fcmService.sendMessage(
//                    request.getUserId(),
//                    request.getTitle(),
//                    request.getBody(),
//                    request.getData()
//            );
//
//            return ResponseEntity.ok(new NotificationResponse(true, messageId, "알림이 성공적으로 전송되었습니다."));
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body(new NotificationResponse(false, null, "알림 전송 실패: " + e.getMessage()));
//        }
//    }


    @Operation(summary = "FCM 토큰 삭제")
    @DeleteMapping("/token/delete/{userId}")
    public ResponseEntity<Void> deleteFcmToken(@PathVariable Integer userId) {
        fcmTokenService.deleteFcmToken(userId);
        return ResponseEntity.ok().build();
    }
}