package com.d208.mr_patent_backend.domain.voca.dto.level;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 레벨 정보를 담는 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LevelDTO {
    private Byte level_id;
    private String level_name;
    private boolean accessible;
    private Byte best_score;
    private boolean passed;
}