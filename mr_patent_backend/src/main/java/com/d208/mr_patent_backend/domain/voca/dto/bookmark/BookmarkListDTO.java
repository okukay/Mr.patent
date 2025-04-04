package com.d208.mr_patent_backend.domain.voca.dto.bookmark;

import com.d208.mr_patent_backend.domain.voca.dto.BookmarkDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 북마크 목록을 담는 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookmarkListDTO {
    private List<BookmarkDTO> bookmarks;
    private Integer total;
}