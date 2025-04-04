package com.d208.mr_patent_backend.domain.voca.dto.level;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 단어 정보를 담는 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WordDTO {
    private Long word_id;
    private String word_name;
    private String word_mean;
    private boolean is_bookmarked;
}