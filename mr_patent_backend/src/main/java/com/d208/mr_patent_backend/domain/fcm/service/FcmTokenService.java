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

    public void saveOrUpdateToken(Integer userId, String token) {
        FcmToken fcmToken = fcmTokenRepository.findByUserId(userId)
                .map(existing -> {
                    existing.setToken(token);
                    existing.setUpdatedAt(LocalDateTime.now());
                    return existing;
                })
                .orElse(FcmToken.builder()
                        .userId(userId)
                        .token(token)
                        .updatedAt(LocalDateTime.now())
                        .build());

        fcmTokenRepository.save(fcmToken);
    }

    public String getTokenByUserId(Integer userId) {
        return fcmTokenRepository.findByUserId(userId)
                .map(FcmToken::getToken)
                .orElse(null);
    }
}
