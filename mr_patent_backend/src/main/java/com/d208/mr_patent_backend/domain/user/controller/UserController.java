package com.d208.mr_patent_backend.domain.user.controller;

import com.d208.mr_patent_backend.domain.user.dto.UserSignupRequestDTO;
import com.d208.mr_patent_backend.domain.user.dto.ExpertSignupRequestDTO;
import com.d208.mr_patent_backend.domain.user.service.UserService;
import com.d208.mr_patent_backend.domain.s3.service.S3Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.extern.slf4j.Slf4j;
import com.d208.mr_patent_backend.global.jwt.TokenInfo;

import jakarta.validation.Valid;

import com.d208.mr_patent_backend.domain.user.dto.LoginRequestDTO;
import com.d208.mr_patent_backend.domain.user.dto.TokenRequestDTO;
import com.d208.mr_patent_backend.domain.user.dto.UserInfoResponseDTO;
import com.d208.mr_patent_backend.domain.user.dto.UserUpdateRequestDTO;
import com.d208.mr_patent_backend.domain.user.dto.UserUpdateResponseDTO;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;

@Tag(name = "회원 API", description = "로그인/회원가입 및 개인 정보")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;
    private final S3Service s3Service;

    @Operation(summary = "일반 회원 가입")
    @PostMapping("")
    public ResponseEntity<Map<String, Object>> signUpUser(@Valid @RequestBody UserSignupRequestDTO requestDto) {
        try {
            userService.signUpUser(requestDto);
            Map<String, Object> response = new HashMap<>();
            Map<String, String> data = new HashMap<>();
            data.put("message", "회원가입이 완료되었습니다.");
            response.put("data", data);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            Map<String, String> data = new HashMap<>();
            data.put("message", e.getMessage());
            response.put("data", data);
            return ResponseEntity.badRequest().body(response);
        }
    }

    @Operation(summary = "변리사 회원 가입")
    @PostMapping("/expert")
    public ResponseEntity<Map<String, Object>> signUpExpert(@Valid @RequestBody ExpertSignupRequestDTO requestDto) {
        try {
            userService.signUpExpert(requestDto);
            Map<String, Object> response = new HashMap<>();
            Map<String, String> data = new HashMap<>();
            data.put("message", "변리사 회원가입이 완료되었습니다.");
            response.put("data", data);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            Map<String, String> data = new HashMap<>();
            data.put("message", e.getMessage());
            response.put("data", data);
            return ResponseEntity.badRequest().body(response);
        }
    }

    @Operation(summary = "회원 로그인")
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequestDTO requestDto) {
        try {
            TokenInfo tokenInfo = userService.login(requestDto);
            Map<String, Object> response = new HashMap<>();
            response.put("data", tokenInfo);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            Map<String, String> data = new HashMap<>();
            data.put("message", e.getMessage());
            response.put("data", data);
            return ResponseEntity.badRequest().body(response);
        }
    }

    @Operation(summary = "회원 로그아웃")
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(@RequestHeader("Authorization") String token) {
        try {
            String accessToken = token.substring(7);
            userService.logout(accessToken);
            Map<String, Object> response = new HashMap<>();
            Map<String, String> data = new HashMap<>();
            data.put("message", "로그아웃이 완료되었습니다.");
            response.put("data", data);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            Map<String, String> data = new HashMap<>();
            data.put("message", e.getMessage());
            response.put("data", data);
            return ResponseEntity.badRequest().body(response);
        }
    }

    @Operation(summary = "토큰 재발급")
    @PostMapping("/reissue")
    public ResponseEntity<Map<String, Object>> reissue(@RequestBody TokenRequestDTO tokenRequestDTO) {
        try {
            TokenInfo tokenInfo = userService.reissue(tokenRequestDTO.getRefreshToken());
            Map<String, Object> response = new HashMap<>();
            response.put("data", tokenInfo);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            Map<String, String> data = new HashMap<>();
            data.put("message", "토큰 재발급에 실패했습니다.");
            response.put("data", data);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

    @Operation(summary = "회원 개인 정보 조회")
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getUserInfo() {
        UserInfoResponseDTO userInfo = userService.getUserInfo();

        Map<String, Object> response = new HashMap<>();
        response.put("data", userInfo);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "회원 개인 정보 수정")
    @PatchMapping("/me")
    public ResponseEntity<Map<String, Object>> updateUserInfo(
            @Valid @RequestBody UserUpdateRequestDTO requestDto) {
        UserUpdateResponseDTO responseDto = userService.updateUserInfo(requestDto);

        Map<String, Object> response = new HashMap<>();
        response.put("data", responseDto);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "회원 탈퇴")
    @DeleteMapping("/me")
    public ResponseEntity<Map<String, Object>> deleteUser() {
        userService.deleteUser();

        Map<String, Object> response = new HashMap<>();
        Map<String, String> data = new HashMap<>();
        data.put("message", "회원 탈퇴가 완료되었습니다.");
        response.put("data", data);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "비밀번호 변경")
    @PatchMapping("/me/pw")
    public ResponseEntity<Map<String, Object>> updatePassword(@RequestBody Map<String, String> request) {
        String currentPassword = request.get("current_password");
        String newPassword = request.get("new_password");

        if (currentPassword == null || currentPassword.trim().isEmpty()) {
            throw new RuntimeException("현재 비밀번호는 필수 입력값입니다.");
        }
        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new RuntimeException("새 비밀번호는 필수 입력값입니다.");
        }

        userService.updatePassword(currentPassword, newPassword);

        Map<String, Object> response = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        data.put("message", "비밀번호가 성공적으로 변경되었습니다.");
        data.put("user_updated_at", LocalDateTime.now());
        response.put("data", data);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "프로필 이미지 업로드용 Presigned URL 발급")
    @GetMapping("/profile-image/upload-url")
    public ResponseEntity<Map<String, Object>> getProfileImageUploadUrl(
            @RequestParam String filename,
            @RequestParam String contenttype
    ) {
        String presignedUrl = s3Service.generatePresignedUploadUrl(filename, contenttype);
        Map<String, Object> response = new HashMap<>();
        Map<String, String> data = new HashMap<>();
        data.put("url", presignedUrl);
        response.put("data", data);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "프로필 이미지 다운로드/조회용 Presigned URL 발급")
    @GetMapping("/profile-image/download-url")
    public ResponseEntity<Map<String, Object>> getProfileImageDownloadUrl(
            @RequestParam String filename
    ) {
        String presignedUrl = s3Service.generatePresignedDownloadUrl(filename);
        Map<String, Object> response = new HashMap<>();
        Map<String, String> data = new HashMap<>();
        data.put("url", presignedUrl);
        response.put("data", data);
        return ResponseEntity.ok(response);
    }
}
