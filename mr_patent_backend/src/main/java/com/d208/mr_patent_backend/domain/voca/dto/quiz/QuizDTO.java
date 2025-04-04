package com.d208.mr_patent_backend.domain.voca.dto.quiz;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 퀴즈 문제 정보를 담는 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizDTO {
    private Byte level_id;
    private List<QuestionDTO> questions;
}