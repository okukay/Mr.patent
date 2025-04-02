package com.d208.mr_patent_backend.domain.user.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class UserUpdateRequestDTO {
    private String userName;
    private String userImage;

    // 변리사 추가 정보
    private String expertDescription;
    private String expertAddress;
    private String expertPhone;
    private List<ExpertCategoryDTO> expertCategories;
}