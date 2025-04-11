package com.d208.mr_patent_backend.domain.voca.dto.bookmark;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 북마크 응답을 담는 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookmarkResponseDTO {
    private Long bookmark_id;
}