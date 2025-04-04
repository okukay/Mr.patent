package com.d208.mr_patent_backend.domain.voca.dto.quiz;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 퀴즈 문제 항목을 담는 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionDTO {
    private Long question_id;
    private String question_text;
    private List<OptionDTO> options;
    private Long correct_option;
}