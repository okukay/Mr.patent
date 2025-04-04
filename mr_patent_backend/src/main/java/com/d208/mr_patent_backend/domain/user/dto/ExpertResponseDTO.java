package com.d208.mr_patent_backend.domain.user.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ExpertResponseDTO {
    private Integer expertId;
    private String userName;        // 변리사 이름
    private String userEmail;       // 변리사 이메일
    private String expertDescription; // 변리사 소개
    private String expertAddress;     // 사무실 주소
    private String expertPhone;       // 연락처
    private LocalDate expertGetDate;  // 자격 취득일
    private List<String> expertCategories; // 전문 분야 카테고리들
}