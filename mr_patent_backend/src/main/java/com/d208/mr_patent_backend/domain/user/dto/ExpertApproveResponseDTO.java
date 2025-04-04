package com.d208.mr_patent_backend.domain.user.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ExpertApproveResponseDTO {
    private Integer expertId;
    private String userEmail;
    private String userName;
    private LocalDate expertGetDate;
    private Integer expertStatus;
    private String expertLicense;
    private List<String> expertCategories;
    private LocalDateTime expertCreatedAt;
}