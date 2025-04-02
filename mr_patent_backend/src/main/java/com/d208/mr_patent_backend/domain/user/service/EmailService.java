package com.d208.mr_patent_backend.domain.user.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender emailSender;
    private final Map<String, AuthCode> authCodeMap = new ConcurrentHashMap<>();

    @Value("${spring.mail.auth-code-expiration-millis}")
    private long authCodeExpirationMillis;

    // ConcurrentHashMap.newKeySet()을 사용하여 thread-safe한 Set 생성
    private final Set<String> verifiedEmails = ConcurrentHashMap.newKeySet();

    // 중복 확인된 이메일 저장용 Set
    private final Set<String> checkedEmails = ConcurrentHashMap.newKeySet();

    @Data
    @AllArgsConstructor
    private static class AuthCode {
        private String code;
        private LocalDateTime expiredAt;
    }

    public void sendAuthEmail(String email) {
        String authCode = createAuthCode();
        LocalDateTime expiredAt = LocalDateTime.now().plusMinutes(3);

        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(email);
            helper.setSubject("MR.Patent 이메일 인증");
            helper.setText(createEmailContent(authCode), true);

            emailSender.send(message);
            authCodeMap.put(email, new AuthCode(authCode, expiredAt));

            log.info("인증 메일 발송 완료: {}", email);
        } catch (MessagingException e) {
            log.error("메일 발송 실패: {}", e.getMessage());
            throw new RuntimeException("메일 발송에 실패했습니다.");
        }
    }

    private String createEmailContent(String authCode) {
        return String.format(
                "<div style='margin:20px;'>" +
                        "<h1>MR.Patent 이메일 인증</h1>" +
                        "<p>안녕하세요. MR.Patent 이메일 인증 번호입니다.</p>" +
                        "<p>아래의 인증 번호를 입력해주세요.</p>" +
                        "<div style='font-size:130%%'>" +
                        "인증번호: <strong>%s</strong>" +
                        "</div><br>" +
                        "<p>감사합니다.</p>" +
                        "</div>",
                authCode
        );
    }

    private String createAuthCode() {
        int length = 6;
        Random random = new Random();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < length; i++) {
            builder.append(random.nextInt(10));
        }
        return builder.toString();
    }

    // 인증 완료 확인 메서드 추가
    public boolean isVerifiedEmail(String email) {
        return verifiedEmails.contains(email);
    }

    // 인증 성공 시 이메일 저장
    public boolean verifyAuthCode(String email, String authCode) {
        AuthCode savedAuthCode = authCodeMap.get(email);
        if (savedAuthCode == null) {
            return false;
        }

        if (LocalDateTime.now().isAfter(savedAuthCode.getExpiredAt())) {
            authCodeMap.remove(email);
            return false;
        }

        boolean isValid = savedAuthCode.getCode().equals(authCode);
        if (isValid) {
            authCodeMap.remove(email);
            verifiedEmails.add(email);  // 인증 성공한 이메일 저장
        }
        return isValid;
    }

    // 중복 확인 완료 표시 메서드
    public void setEmailChecked(String email) {
        checkedEmails.add(email);
    }

    // 중복 확인 여부 확인 메서드
    public boolean isEmailChecked(String email) {
        return checkedEmails.contains(email);
    }

    public void sendPasswordResetEmail(String email) {
        String authCode = createAuthCode();
        LocalDateTime expiredAt = LocalDateTime.now().plusMinutes(3);

        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setTo(email);
            helper.setSubject("MR.Patent 비밀번호 재설정");
            helper.setText(createPasswordResetEmailContent(authCode), true);

            emailSender.send(message);
            authCodeMap.put(email, new AuthCode(authCode, expiredAt));

            log.info("비밀번호 재설정 메일 발송 완료: {}", email);
        } catch (MessagingException e) {
            log.error("메일 발송 실패: {}", e.getMessage());
            throw new RuntimeException("메일 발송에 실패했습니다.");
        }
    }

    private String createPasswordResetEmailContent(String authCode) {
        return String.format(
                "<div style='margin:20px;'>" +
                        "<h1>MR.Patent 비밀번호 재설정</h1>" +
                        "<p>안녕하세요. MR.Patent 비밀번호 재설정을 위한 인증번호입니다.</p>" +
                        "<p>아래의 인증 번호를 입력해주세요.</p>" +
                        "<div style='font-size:130%%'>" +
                        "인증번호: <strong>%s</strong>" +
                        "</div><br>" +
                        "<p>감사합니다.</p>" +
                        "</div>",
                authCode
        );
    }

    public void removeVerifiedEmail(String email) {
        verifiedEmails.remove(email);
    }
}
