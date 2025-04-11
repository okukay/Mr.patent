package com.d208.mr_patent_backend.domain.user.dto;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class UserInfoResponseDTO {
    private Integer userId;
    private String userEmail;
    private String userName;
    private String userImage;
    private Integer userRole;

    // 변리사 추가 정보 (선택적)
    private Integer expertId;
    private String expertDescription;
    private String expertAddress;
    private String expertPhone;
    private LocalDate expertGetDate;
    private List<ExpertCategoryDTO> expertCategories;
}