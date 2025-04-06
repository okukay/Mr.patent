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
public class ExpertDetailResponseDTO {
    private Integer expertId;
    private Integer userId;
    private String userName;
    private String expertDescription;
    private String expertAddress;
    private String expertPhone;
    private LocalDate expertGetDate;
    private LocalDateTime expertCreatedAt;
    private String userEmail;
    private String userImage;
    private List<ExpertCategoryDTO> expertCategories;
}