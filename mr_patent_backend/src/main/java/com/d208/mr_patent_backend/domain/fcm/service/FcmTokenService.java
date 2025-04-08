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
        FcmToken fcmToken = fcmTokenRepository.findByToken(token);

        //이미 존재한다면
        if (fcmToken != null) {
            fcmToken.setUserId(userId);
            fcmToken.setUpdatedAt(LocalDateTime.now());

        } else {
            // 없는 경우 → 새로 insert
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
