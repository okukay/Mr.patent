package com.d208.mr_patent_backend.global.util;

import com.d208.mr_patent_backend.domain.user.entity.User;
import com.d208.mr_patent_backend.domain.user.repository.UserRepository;
import com.d208.mr_patent_backend.global.jwt.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityUtils {
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    public User getCurrentUser(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7); // "Bearer " 제거
                Integer userId = jwtTokenProvider.getUserId(token);
                return userRepository.findById(userId)
                        .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
            } catch (Exception e) {
                // 토큰 추출 실패 시 처리
                throw new RuntimeException("유효하지 않은 인증 정보입니다: " + e.getMessage());
            }
        }
        throw new RuntimeException("인증 정보가 없습니다.");
    }
}