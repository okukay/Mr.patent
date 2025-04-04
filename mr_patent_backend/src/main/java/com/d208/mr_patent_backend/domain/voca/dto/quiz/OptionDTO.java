package com.d208.mr_patent_backend.domain.voca.dto.quiz;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 퀴즈 선택지를 담는 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OptionDTO {
    private Long option_id;
    private String option_text;
}