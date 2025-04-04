package com.d208.mr_patent_backend.domain.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.d208.mr_patent_backend.domain.user.service.EmailService;
import com.d208.mr_patent_backend.domain.user.service.UserService;
import com.d208.mr_patent_backend.domain.user.dto.EmailAvailableResponseDTO;
import com.d208.mr_patent_backend.domain.user.dto.EmailVerificationResponseDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "이메일 API", description = "이메일 전송 및 인증")
@RestController
@RequestMapping("/api/email")
@RequiredArgsConstructor
@Slf4j
public class EmailController {

    private final EmailService emailService;
    private final UserService userService;

    @Operation(summary = "이메일 중복 확인")
    @GetMapping("/check")
    public ResponseEntity<Map<String, Object>> checkEmailDuplicate(@RequestParam String email) {
        try {
            boolean isDuplicate = userService.checkEmailDuplicate(email);
            Map<String, Object> response = new HashMap<>();
            response.put("data", EmailAvailableResponseDTO.of(!isDuplicate));  // DB에 없으면 available true

            if (!isDuplicate) {  // DB에 없는 경우에만 체크 표시
                emailService.setEmailChecked(email);
            }

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("이메일 중복 체크 실패: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            Map<String, String> data = new HashMap<>();
            data.put("message", e.getMessage());
            response.put("data", data);
            return ResponseEntity.badRequest().body(response);
        }
    }

    @Operation(summary = "이메일 인증 번호 전송")
    @PostMapping("/request")
    public ResponseEntity<Map<String, Object>> sendAuthCode(@RequestParam String email) {
        try {
            // 중복 확인을 했는지 체크
            if (!emailService.isEmailChecked(email)) {
                Map<String, Object> errorResponse = new HashMap<>();
                Map<String, String> data = new HashMap<>();
                data.put("message", "먼저 중복 확인을 해 주세요.");
                errorResponse.put("data", data);
                return ResponseEntity.badRequest().body(errorResponse);
            }

            emailService.sendAuthEmail(email);

            Map<String, Object> response = new HashMap<>();
            Map<String, String> data = new HashMap<>();
            data.put("message", "인증 코드가 발송되었습니다.");
            response.put("data", data);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            Map<String, String> data = new HashMap<>();
            data.put("message", e.getMessage());
            errorResponse.put("data", data);
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @Operation(summary = "이메일 인증")
    @PostMapping("/verification")
    public ResponseEntity<Map<String, Object>> verifyAuthCode(
            @RequestParam String email,
            @RequestParam String authCode) {
        boolean isValid = emailService.verifyAuthCode(email, authCode);
        Map<String, Object> response = new HashMap<>();

        if (isValid) {
            response.put("data", EmailVerificationResponseDTO.of(email));
            return ResponseEntity.ok(response);
        }

        Map<String, String> data = new HashMap<>();
        data.put("message", "인증번호가 올바르지 않거나 만료되었습니다.");
        response.put("data", data);
        return ResponseEntity.badRequest().body(response);
    }

    @Operation(summary = "이메일 인증 번호 전송(비밀번호 변경)")
    @PostMapping("/password/forgot")
    public ResponseEntity<Map<String, Object>> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("user_email");
        if (email == null || email.trim().isEmpty()) {
            throw new RuntimeException("이메일은 필수 입력값입니다.");
        }

        userService.sendPasswordResetEmail(email);

        Map<String, Object> response = new HashMap<>();
        Map<String, String> data = new HashMap<>();
        data.put("message", "비밀번호 재설정 이메일이 전송되었습니다.");
        response.put("data", data);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "이메일 인증(비밀번호 변경)")
    @PostMapping("/password/verification")
    public ResponseEntity<Map<String, Object>> verifyPasswordCode(@RequestBody Map<String, String> request) {
        String email = request.get("user_email");
        String authCode = request.get("verification_code");

        if (email == null || email.trim().isEmpty()) {
            throw new RuntimeException("이메일은 필수 입력값입니다.");
        }
        if (authCode == null || authCode.trim().isEmpty()) {
            throw new RuntimeException("인증 코드는 필수 입력값입니다.");
        }

        userService.verifyPasswordCode(email, authCode);

        Map<String, Object> response = new HashMap<>();
        Map<String, String> data = new HashMap<>();
        data.put("message", "이메일 인증이 완료되었습니다.");
        response.put("data", data);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "인증 후 비밀번호 변경")
    @PostMapping("/password/reset")
    public ResponseEntity<Map<String, Object>> resetPassword(@RequestBody Map<String, String> request) {
        String email = request.get("user_email");
        String newPassword = request.get("new_password");

        if (email == null || email.trim().isEmpty()) {
            throw new RuntimeException("이메일은 필수 입력값입니다.");
        }
        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new RuntimeException("새 비밀번호는 필수 입력값입니다.");
        }

        // 이메일 인증 여부 확인 후 비밀번호 변경
        userService.resetPassword(email, newPassword);

        Map<String, Object> response = new HashMap<>();
        Map<String, String> data = new HashMap<>();
        data.put("message", "비밀번호가 성공적으로 재설정되었습니다.");
        response.put("data", data);

        return ResponseEntity.ok(response);
    }
}