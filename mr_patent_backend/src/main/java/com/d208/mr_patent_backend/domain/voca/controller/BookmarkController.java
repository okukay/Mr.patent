package com.d208.mr_patent_backend.domain.voca.controller;

import com.d208.mr_patent_backend.domain.voca.dto.BookmarkCountDTO;
import com.d208.mr_patent_backend.domain.voca.dto.bookmark.BookmarkListDTO;
import com.d208.mr_patent_backend.domain.voca.dto.BookmarkRequestDTO;
import com.d208.mr_patent_backend.domain.voca.dto.BookmarkResponseDTO;
import com.d208.mr_patent_backend.domain.voca.service.BookmarkService;
import com.d208.mr_patent_backend.global.dto.CommonResponseDto;
import com.d208.mr_patent_backend.global.jwt.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 북마크 추가
     */
    @PostMapping("")
    public ResponseEntity<CommonResponseDto<BookmarkResponseDTO>> addBookmark(
            Authentication authentication,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody BookmarkRequestDTO request) {

        String userEmail;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            userEmail = jwtTokenProvider.getUserEmail(token);
        } else {
            throw new RuntimeException("인증 정보를 찾을 수 없습니다.");
        }

        BookmarkResponseDTO result = bookmarkService.addBookmark(userEmail, request.getWord_id());
        return ResponseEntity.ok(CommonResponseDto.success(result));
    }

    /**
     * 북마크 삭제
     */
    @DeleteMapping("/{bookmark_id}")
    public ResponseEntity<CommonResponseDto<Object>> deleteBookmark(
            Authentication authentication,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable("bookmark_id") Long bookmarkId) {

        String userEmail;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            userEmail = jwtTokenProvider.getUserEmail(token);
        } else {
            throw new RuntimeException("인증 정보를 찾을 수 없습니다.");
        }

        bookmarkService.deleteBookmark(userEmail, bookmarkId);
        return ResponseEntity.ok(CommonResponseDto.success(null));
    }

    /**
     * 레벨별 북마크 개수 조회
     */
    @GetMapping("/count")
    public ResponseEntity<CommonResponseDto<Map<String, List<BookmarkCountDTO>>>> getBookmarkCount(
            Authentication authentication,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "level_id", required = false) Byte levelId) {

        String userEmail;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            userEmail = jwtTokenProvider.getUserEmail(token);
        } else {
            throw new RuntimeException("인증 정보를 찾을 수 없습니다.");
        }

        List<BookmarkCountDTO> counts = bookmarkService.getBookmarkCounts(userEmail, levelId);
        Map<String, List<BookmarkCountDTO>> responseData = Map.of("levels", counts);
        return ResponseEntity.ok(CommonResponseDto.success(responseData));
    }

    /**
     * 북마크 목록 조회
     */
    @GetMapping("")
    public ResponseEntity<CommonResponseDto<BookmarkListDTO>> getBookmarks(
            Authentication authentication,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "level_id", required = false) Byte levelId) {

        String userEmail;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            userEmail = jwtTokenProvider.getUserEmail(token);
        } else {
            throw new RuntimeException("인증 정보를 찾을 수 없습니다.");
        }

        BookmarkListDTO bookmarks = bookmarkService.getBookmarks(userEmail, levelId);
        return ResponseEntity.ok(CommonResponseDto.success(bookmarks));
    }
}