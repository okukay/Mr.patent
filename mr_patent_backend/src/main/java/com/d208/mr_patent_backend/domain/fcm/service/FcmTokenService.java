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
        FcmToken fToken = fcmTokenRepository.findByUserId(userId).orElse(null);

        if (fToken != null) {
            // 기존 토큰이 있는 경우 → 업데이트
            fToken.setToken(token);
            fToken.setUpdatedAt(LocalDateTime.now());
        } else {
            // 기존 토큰이 없는 경우 → 새로 생성
            fToken = FcmToken.builder()
                    .userId(userId)
                    .token(token)
                    .updatedAt(LocalDateTime.now())
                    .build();
        }

        fcmTokenRepository.save(fToken);
    }

    // userId 에 따른 토큰 가져오기
    public String getTokenByUserId(Integer userId) {
        return fcmTokenRepository.findByUserId(userId)
                .map(FcmToken::getToken)
                .orElse(null);
    }
}
