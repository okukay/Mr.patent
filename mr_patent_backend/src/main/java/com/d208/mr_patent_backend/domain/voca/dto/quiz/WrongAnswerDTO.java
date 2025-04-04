package com.d208.mr_patent_backend.domain.voca.dto.quiz;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 틀린 문제 정보를 담는 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WrongAnswerDTO {
    private Long question_id;
    private String question_text;
    private String correct_option_text;
}