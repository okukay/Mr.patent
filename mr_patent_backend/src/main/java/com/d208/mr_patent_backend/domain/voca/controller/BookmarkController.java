package com.d208.mr_patent_backend.domain.voca.controller;

import com.d208.mr_patent_backend.domain.user.entity.User;
import com.d208.mr_patent_backend.domain.voca.dto.BookmarkCountDTO;
import com.d208.mr_patent_backend.domain.voca.dto.bookmark.BookmarkListDTO;
import com.d208.mr_patent_backend.domain.voca.dto.BookmarkRequestDTO;
import com.d208.mr_patent_backend.domain.voca.dto.BookmarkResponseDTO;
import com.d208.mr_patent_backend.domain.voca.service.BookmarkService;
import com.d208.mr_patent_backend.global.dto.CommonResponseDto;
import com.d208.mr_patent_backend.global.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "북마크 API", description = "단어 북마크 추가, 조회, 삭제 기능")
@RestController
@RequestMapping("/api/bookmarks")
@RequiredArgsConstructor
@Slf4j
public class BookmarkController {

    private final BookmarkService bookmarkService;
    private final SecurityUtils securityUtils;

    /**
     * 북마크 추가
     */
    @Operation(summary = "단어 북마크 추가")
    @PostMapping("")
    public ResponseEntity<CommonResponseDto<BookmarkResponseDTO>> addBookmark(
            HttpServletRequest request,
            @RequestBody BookmarkRequestDTO bookmarkRequest) {

        User user = securityUtils.getCurrentUser(request);

        BookmarkResponseDTO result = bookmarkService.addBookmark(user.getUserEmail(), bookmarkRequest.getWord_id());
        return ResponseEntity.ok(CommonResponseDto.success(result));
    }

    /**
     * 북마크 삭제
     */
    @Operation(summary = "단어 북마크 삭제")
    @DeleteMapping("/{bookmark_id}")
    public ResponseEntity<CommonResponseDto<Object>> deleteBookmark(
            HttpServletRequest request,
            @PathVariable("bookmark_id") Long bookmarkId) {

        User user = securityUtils.getCurrentUser(request);

        bookmarkService.deleteBookmark(user.getUserEmail(), bookmarkId);
        return ResponseEntity.ok(CommonResponseDto.success(null));
    }

    /**
     * 레벨별 북마크 개수 조회
     */
    @Operation(summary = "레벨별 북마크 개수 조회")
    @GetMapping("/count")
    public ResponseEntity<CommonResponseDto<Map<String, List<BookmarkCountDTO>>>> getBookmarkCount(
            HttpServletRequest request,
            @RequestParam(value = "level_id", required = false) Byte levelId) {

        User user = securityUtils.getCurrentUser(request);

        List<BookmarkCountDTO> counts = bookmarkService.getBookmarkCounts(user.getUserEmail(), levelId);
        Map<String, List<BookmarkCountDTO>> responseData = Map.of("levels", counts);
        return ResponseEntity.ok(CommonResponseDto.success(responseData));
    }

    /**
     * 북마크 목록 조회
     */
    @Operation(summary = "북마크 목록 조회")
    @GetMapping("")
    public ResponseEntity<CommonResponseDto<BookmarkListDTO>> getBookmarks(
            HttpServletRequest request,
            @RequestParam(value = "level_id", required = false) Byte levelId) {

        User user = securityUtils.getCurrentUser(request);

        BookmarkListDTO bookmarks = bookmarkService.getBookmarks(user.getUserEmail(), levelId);
        return ResponseEntity.ok(CommonResponseDto.success(bookmarks));
    }
}