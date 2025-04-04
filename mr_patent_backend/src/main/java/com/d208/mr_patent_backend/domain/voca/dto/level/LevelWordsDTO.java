package com.d208.mr_patent_backend.domain.voca.dto.level;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 레벨별 단어 목록을 담는 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LevelWordsDTO {
    private Byte level_id;
    private String level_name;
    private List<WordDTO> words;
}