package com.d208.mr_patent_backend.domain.user.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ExpertApproveRequestDTO {
    private Integer status;  // 0: 미승인, 1: 승인
}