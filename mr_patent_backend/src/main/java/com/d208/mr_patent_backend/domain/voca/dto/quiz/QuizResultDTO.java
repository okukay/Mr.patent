package com.d208.mr_patent_backend.domain.voca.dto.quiz;

import com.d208.mr_patent_backend.domain.voca.dto.level.WordDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 퀴즈 결과를 담는 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizResultDTO {
    private Byte level_id;
    private Byte score;
    private List<WrongAnswerDTO> wrong_answers;
}