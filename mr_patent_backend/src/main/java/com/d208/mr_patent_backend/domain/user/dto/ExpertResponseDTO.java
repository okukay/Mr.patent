package com.d208.mr_patent_backend.domain.user.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
public class ExpertResponseDTO {
    private Integer expertId;
    private String name;        // 변리사 이름
    private String email;       // 변리사 이메일
    private String description; // 변리사 소개
    private String address;     // 사무실 주소
    private String phone;       // 연락처
    private String license;     // 자격증 이미지 URL
    private LocalDate getDate;  // 자격 취득일
    private List<String> categories; // 전문 분야 카테고리들
}