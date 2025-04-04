package com.d208.mr_patent_backend.domain.voca.dto.quiz;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 퀴즈 제출 요청을 담는 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizSubmitRequest {
    private List<AnswerDTO> answers;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnswerDTO {
        private Long word_id;
    }
}