package com.d208.mr_patent_backend.domain.user.service;

import com.d208.mr_patent_backend.domain.user.dto.UserSignupRequestDTO;
import com.d208.mr_patent_backend.domain.user.dto.ExpertCategoryDTO;
import com.d208.mr_patent_backend.domain.user.dto.ExpertSignupRequestDTO;
import com.d208.mr_patent_backend.domain.user.entity.User;
import com.d208.mr_patent_backend.domain.user.entity.Expert;
import com.d208.mr_patent_backend.domain.user.repository.UserRepository;
import com.d208.mr_patent_backend.domain.user.repository.ExpertRepository;
import com.d208.mr_patent_backend.domain.category.entity.ExpertCategory;
import com.d208.mr_patent_backend.domain.category.entity.Category;
import com.d208.mr_patent_backend.domain.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import com.d208.mr_patent_backend.global.jwt.TokenInfo;
import com.d208.mr_patent_backend.global.jwt.JwtTokenProvider;
import com.d208.mr_patent_backend.domain.user.dto.LoginRequestDTO;
import com.d208.mr_patent_backend.domain.user.dto.UserInfoResponseDTO;
import org.springframework.security.core.context.SecurityContextHolder;
import com.d208.mr_patent_backend.domain.user.dto.UserUpdateRequestDTO;
import com.d208.mr_patent_backend.domain.user.dto.UserUpdateResponseDTO;
import java.time.LocalDateTime;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final ExpertRepository expertRepository;
    private final CategoryRepository categoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public void signUpUser(UserSignupRequestDTO requestDto) {
        String email = requestDto.getUserEmail();

        // 이메일 중복 체크
        if (userRepository.existsByUserEmail(email)) {
            throw new RuntimeException("이미 존재하는 이메일입니다.");
        }

        // 이메일 인증 확인
        if (!emailService.isVerifiedEmail(email)) {
            throw new RuntimeException("이메일 인증이 필요합니다.");
        }

        // User 엔티티 생성 및 저장
        User user = new User();
        user.setUserEmail(requestDto.getUserEmail());
        user.setUserPw(passwordEncoder.encode(requestDto.getUserPw()));
        user.setUserName(requestDto.getUserName());
        user.setUserImage(requestDto.getUserImage());
        user.setUserRole(requestDto.getUserRole());

        userRepository.save(user);
    }

    @Transactional
    public void signUpExpert(ExpertSignupRequestDTO requestDto) {
        String email = requestDto.getUserEmail();

        // 이메일 중복 체크 확인
        if (!emailService.isEmailChecked(email)) {
            throw new RuntimeException("이메일 중복 확인이 필요합니다.");
        }

        // 이메일 인증 확인
        if (!emailService.isVerifiedEmail(email)) {
            throw new RuntimeException("이메일 인증이 필요합니다.");
        }

        // User 엔티티 생성 및 저장
        User user = new User();
        user.setUserEmail(requestDto.getUserEmail());
        user.setUserPw(passwordEncoder.encode(requestDto.getUserPw()));
        user.setUserName(requestDto.getUserName());
        user.setUserImage(requestDto.getUserImage());
        user.setUserRole(requestDto.getUserRole());

        userRepository.save(user);

        // Expert 엔티티 생성 및 저장
        Expert expert = new Expert();
        expert.setUser(user);
        expert.setExpertIdentification(requestDto.getExpertIdentification());
        expert.setExpertDescription(requestDto.getExpertDescription());
        expert.setExpertAddress(requestDto.getExpertAddress());
        expert.setExpertPhone(requestDto.getExpertPhone());
        expert.setExpertLicense(requestDto.getExpertLicense());
        expert.setExpertGetDate(requestDto.getExpertGetDate());

        // ExpertCategory 설정
        if (requestDto.getExpertCategories() != null && !requestDto.getExpertCategories().isEmpty()) {
            for (ExpertCategoryDTO categoryDto : requestDto.getExpertCategories()) {
                Category category = categoryRepository.findByCategoryName(categoryDto.getCategoryName())
                        .orElseThrow(() -> new RuntimeException("존재하지 않는 카테고리입니다: " + categoryDto.getCategoryName()));

                ExpertCategory newExpertCategory = new ExpertCategory();
                newExpertCategory.setCategory(category);
                expert.addExpertCategory(newExpertCategory);
            }
        }

        expertRepository.save(expert);
    }

    // 이메일 중복 체크
    public boolean checkEmailDuplicate(String email) {
        return userRepository.existsByUserEmail(email);
    }

    @Transactional
    public TokenInfo login(LoginRequestDTO requestDto) {
        // 이메일로 사용자 찾기
        User user = userRepository.findByUserEmail(requestDto.getUserEmail())
                .orElseThrow(() -> new RuntimeException("가입되지 않은 이메일입니다."));

        // 변리사인 경우 승인 상태 확인
        if (user.getUserRole() == 1) {
            Expert expert = expertRepository.findByUser(user)
                    .orElseThrow(() -> new RuntimeException("변리사 정보를 찾을 수 없습니다."));
            if (expert.getExpertStatus() == 0) {
                throw new RuntimeException("아직 승인되지 않은 변리사입니다.");
            }
        }

        // 비밀번호 확인
        if (!passwordEncoder.matches(requestDto.getUserPw(), user.getUserPw())) {
            throw new RuntimeException("비밀번호가 일치하지 않습니다.");
        }

        // 권한 설정
        List<GrantedAuthority> authorities = new ArrayList<>();
        if (user.getUserRole() == 1) {
            authorities.add(new SimpleGrantedAuthority("ROLE_EXPERT"));
        } else if (user.getUserRole() == 2) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        } else {
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        }

        // 인증 객체 생성
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user.getUserEmail(), "", authorities);

        // 토큰 생성
        TokenInfo tokenInfo = TokenInfo.builder()
                .userId(user.getUserId())
                .userEmail(user.getUserEmail())
                .userName(user.getUserName())
                .userRole(user.getUserRole())
                .grantType("Bearer")
                .accessToken(jwtTokenProvider.generateAccessToken(authentication))
                .refreshToken(jwtTokenProvider.generateRefreshToken())
                .build();

        // Refresh Token 저장
        user.setUserRefreshToken(tokenInfo.getRefreshToken());
        userRepository.save(user);

        return tokenInfo;
    }

    @Transactional
    public void logout(String accessToken) {
        // 토큰에서 사용자 이메일 추출
        String userEmail = jwtTokenProvider.getUserEmail(accessToken);

        // 사용자 찾기
        User user = userRepository.findByUserEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // Refresh Token 제거
        user.setUserRefreshToken(null);
        userRepository.save(user);
    }

    @Transactional
    public TokenInfo reissue(String refreshToken) {
        // 리프레시 토큰 검증
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new RuntimeException("유효하지 않은 리프레시 토큰입니다.");
        }

        // DB에서 리프레시 토큰으로 사용자 찾기
        User user = userRepository.findByUserRefreshToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("토큰에 해당하는 사용자가 없습니다."));

        // 권한 설정
        List<GrantedAuthority> authorities = new ArrayList<>();
        if (user.getUserRole() == 1) {
            authorities.add(new SimpleGrantedAuthority("ROLE_EXPERT"));
        } else if (user.getUserRole() == 2) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        } else {
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        }

        // 새로운 토큰 생성
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user.getUserEmail(), "", authorities);

        TokenInfo tokenInfo = TokenInfo.builder()
                .userId(user.getUserId())
                .userEmail(user.getUserEmail())
                .userName(user.getUserName())
                .userRole(user.getUserRole())
                .grantType("Bearer")
                .accessToken(jwtTokenProvider.generateAccessToken(authentication))
                .refreshToken(jwtTokenProvider.generateRefreshToken())
                .build();

        // 리프레시 토큰 업데이트
        user.setUserRefreshToken(tokenInfo.getRefreshToken());
        userRepository.save(user);

        return tokenInfo;
    }

    @Transactional(readOnly = true)
    public UserInfoResponseDTO getUserInfo() {
        // SecurityContext에서 현재 인증된 사용자의 이메일 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();

        // 사용자 찾기
        User user = userRepository.findByUserEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        UserInfoResponseDTO.UserInfoResponseDTOBuilder builder = UserInfoResponseDTO.builder()
                .userId(user.getUserId())
                .userEmail(user.getUserEmail())
                .userName(user.getUserName())
                .userImage(user.getUserImage())
                .userRole(user.getUserRole());

        // 변리사인 경우 추가 정보 조회
        if (user.getUserRole() == 1) {
            Expert expert = expertRepository.findByUser(user)
                    .orElseThrow(() -> new RuntimeException("변리사 정보를 찾을 수 없습니다."));

            List<String> categories = expert.getExpertCategory().stream()
                    .map(ec -> ec.getCategory().getCategoryName())
                    .collect(Collectors.toList());

            builder
                    .expertDescription(expert.getExpertDescription())
                    .expertAddress(expert.getExpertAddress())
                    .expertPhone(expert.getExpertPhone())
                    .expertGetDate(expert.getExpertGetDate())
                    .categories(categories);
        }

        return builder.build();
    }

    @Transactional
    public UserUpdateResponseDTO updateUserInfo(UserUpdateRequestDTO requestDto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();

        User user = userRepository.findByUserEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // null이 아닌 값만 업데이트
        if (requestDto.getUserName() != null) {
            user.setUserName(requestDto.getUserName());
        }
        if (requestDto.getUserImage() != null) {
            user.setUserImage(requestDto.getUserImage());
        }
        user.setUserUpdatedAt(LocalDateTime.now());

        // 변리사인 경우 추가 정보 업데이트
        if (user.getUserRole() == 1) {
            Expert expert = expertRepository.findByUser(user)
                    .orElseThrow(() -> new RuntimeException("변리사 정보를 찾을 수 없습니다."));

            if (requestDto.getExpertDescription() != null) {
                expert.setExpertDescription(requestDto.getExpertDescription());
            }
            if (requestDto.getExpertAddress() != null) {
                expert.setExpertAddress(requestDto.getExpertAddress());
            }
            if (requestDto.getExpertPhone() != null) {
                expert.setExpertPhone(requestDto.getExpertPhone());
            }

            // 카테고리 정보가 포함된 경우에만 카테고리 업데이트
            if (requestDto.getExpertCategories() != null && !requestDto.getExpertCategories().isEmpty()) {
                expert.getExpertCategory().clear();
                for (ExpertCategoryDTO categoryDto : requestDto.getExpertCategories()) {
                    Category category = categoryRepository.findByCategoryName(categoryDto.getCategoryName())
                            .orElseThrow(() -> new RuntimeException("존재하지 않는 카테고리입니다: " + categoryDto.getCategoryName()));

                    ExpertCategory newExpertCategory = new ExpertCategory();
                    newExpertCategory.setCategory(category);
                    expert.addExpertCategory(newExpertCategory);
                }
            }

            expertRepository.save(expert);
        }

        userRepository.save(user);

        return UserUpdateResponseDTO.builder()
                .message("회원정보가 성공적으로 수정되었습니다.")
                .userUpdatedAt(user.getUserUpdatedAt())
                .build();
    }

    @Transactional
    public void deleteUser() {
        // SecurityContext에서 현재 인증된 사용자의 이메일 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();

        User user = userRepository.findByUserEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // 변리사인 경우 Expert 정보도 삭제
        if (user.getUserRole() == 1) {
            Expert expert = expertRepository.findByUser(user)
                    .orElseThrow(() -> new RuntimeException("변리사 정보를 찾을 수 없습니다."));

            // ExpertCategory는 cascade로 자동 삭제됨
            expertRepository.delete(expert);
        }

        // User 삭제
        userRepository.delete(user);
    }

    @Transactional
    public void sendPasswordResetEmail(String email) {
        // 사용자 존재 여부 확인
        if (!userRepository.existsByUserEmail(email)) {
            throw new RuntimeException("등록되지 않은 이메일입니다.");
        }

        emailService.sendPasswordResetEmail(email);
    }

    @Transactional
    public void verifyPasswordCode(String email, String authCode) {
        // 인증 코드 확인
        if (!emailService.verifyAuthCode(email, authCode)) {
            throw new RuntimeException("인증 코드가 유효하지 않습니다.");
        }
    }

    @Transactional
    public void resetPassword(String email, String newPassword) {
        // 이메일 인증 여부 확인
        if (!emailService.isVerifiedEmail(email)) {
            throw new RuntimeException("이메일 인증이 필요합니다.");
        }

        User user = userRepository.findByUserEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // 새 비밀번호로 업데이트
        user.setUserPw(passwordEncoder.encode(newPassword));
        user.setUserUpdatedAt(LocalDateTime.now());

        userRepository.save(user);

        // 인증 완료 후 인증 정보 삭제
        emailService.removeVerifiedEmail(email);
    }

    @Transactional
    public void updatePassword(String currentPassword, String newPassword) {
        // SecurityContext에서 현재 인증된 사용자의 이메일 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();

        User user = userRepository.findByUserEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // 현재 비밀번호 확인
        if (!passwordEncoder.matches(currentPassword, user.getUserPw())) {
            throw new RuntimeException("현재 비밀번호가 일치하지 않습니다.");
        }

        // 새 비밀번호가 현재 비밀번호와 같은지 확인
        if (currentPassword.equals(newPassword)) {
            throw new RuntimeException("새 비밀번호는 현재 비밀번호와 달라야 합니다.");
        }

        // 새 비밀번호로 업데이트
        user.setUserPw(passwordEncoder.encode(newPassword));
        user.setUserUpdatedAt(LocalDateTime.now());

        userRepository.save(user);
    }
}
