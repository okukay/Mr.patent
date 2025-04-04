package com.d208.mr_patent_backend.domain.user.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@Getter
@Setter
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class EmailVerificationResponseDTO {
    private String message;
    private String verified_email;

    public static EmailVerificationResponseDTO of(String email) {
        EmailVerificationResponseDTO response = new EmailVerificationResponseDTO();
        response.setMessage("이메일 인증이 완료되었습니다.");
        response.setVerified_email(email);
        return response;
    }
}