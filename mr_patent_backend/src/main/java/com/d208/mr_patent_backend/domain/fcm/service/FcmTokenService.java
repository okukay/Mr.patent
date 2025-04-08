package com.d208.mr_patent_backend.domain.fcm.service;

import com.d208.mr_patent_backend.domain.fcm.entity.FcmToken;
import com.d208.mr_patent_backend.domain.fcm.repository.FcmTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class FcmTokenService {

    private final FcmTokenRepository fcmTokenRepository;

    // 토큰 저장 또는 업데이트
    public void saveOrUpdateToken(Integer userId, String token) {
        FcmToken fcmToken = fcmTokenRepository.findByUserId(userId).orElse(null);

        if (fcmToken != null) {
            // 기존 토큰이 있는 경우 → 업데이트
            fcmToken.setToken(token);
            fcmToken.setUpdatedAt(LocalDateTime.now());
        } else {
            // 기존 토큰이 없는 경우 → 새로 생성
            fcmToken = FcmToken.builder()
                    .userId(userId)
                    .token(token)
                    .updatedAt(LocalDateTime.now())
                    .build();
        }

        fcmTokenRepository.save(fcmToken);
    }

    // userId 에 따른 토큰 가져오기
    public String getTokenByUserId(Integer userId) {
        return fcmTokenRepository.findByUserId(userId)
                .map(FcmToken::getToken)
                .orElse(null);
    }


    // FCM 토큰 삭제
    public void deleteFcmToken(Integer userId) {
        FcmToken token = fcmTokenRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));
        fcmTokenRepository.delete(token);
    }
}
