package com.d208.mr_patent_backend.domain.voca.dto.level;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WordDTO {
    private Long word_id;
    private String word_name;
    private String word_mean;
    private boolean bookmarked;
    private Long bookmark_id;

    public void setIs_bookmarked(boolean bookmarked) {
        this.bookmarked = bookmarked;
    }

    public void setBookmark_id(Long bookmark_id) {
        this.bookmark_id = bookmark_id;
    }
}