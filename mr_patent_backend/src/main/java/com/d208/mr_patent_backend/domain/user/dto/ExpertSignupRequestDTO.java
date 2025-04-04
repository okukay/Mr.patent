package com.d208.mr_patent_backend.domain.user.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotNull;

@Getter
@Setter
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ExpertSignupRequestDTO {
    @NotBlank(message = "이메일은 필수 입력값입니다.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    private String userEmail;

    @NotBlank(message = "비밀번호는 필수 입력값입니다.")
    private String userPw;

    @NotBlank(message = "이름은 필수 입력값입니다.")
    private String userName;

    private String userImage;
    private Integer userRole;

    @NotBlank(message = "주민등록번호는 필수 입력값입니다.")
    private String expertIdentification;

    @NotBlank(message = "소개는 필수 입력값입니다.")
    private String expertDescription;

    @NotBlank(message = "주소는 필수 입력값입니다.")
    private String expertAddress;

    @NotBlank(message = "전화번호는 필수 입력값입니다.")
    private String expertPhone;

    @NotNull(message = "자격증 발급일자는 필수 입력값입니다.")
    private LocalDate expertGetDate;

    @NotBlank(message = "자격증은 필수 입력값입니다.")
    private String expertLicense;

    private List<ExpertCategoryDTO> expertCategories;
}
