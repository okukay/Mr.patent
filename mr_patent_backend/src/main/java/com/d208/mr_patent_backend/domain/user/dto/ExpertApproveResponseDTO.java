package com.d208.mr_patent_backend.domain.user.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ExpertApproveResponseDTO {
    private Integer expertId;
    private String userEmail;
    private String userName;
    private LocalDate expertGetDate;
    private Integer expertStatus;
    private LocalDateTime createdAt;
}