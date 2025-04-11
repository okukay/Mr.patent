package com.d208.mr_patent_backend.domain.voca.dto.bookmark;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 레벨별 북마크 개수를 담는 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookmarkCountDTO {
    private Byte level_id;
    private Integer count;
}