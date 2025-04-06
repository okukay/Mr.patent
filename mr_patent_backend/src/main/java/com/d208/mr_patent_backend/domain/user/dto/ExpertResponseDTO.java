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
    private String userName;
    private String userEmail;
    private String expertDescription;
    private String expertAddress;
    private String expertPhone;
    private LocalDate expertGetDate;
    private List<ExpertCategoryDTO> expertCategories;
}