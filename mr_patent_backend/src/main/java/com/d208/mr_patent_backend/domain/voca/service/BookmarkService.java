package com.d208.mr_patent_backend.domain.voca.service;

import com.d208.mr_patent_backend.domain.user.entity.User;
import com.d208.mr_patent_backend.domain.user.repository.UserRepository;
import com.d208.mr_patent_backend.domain.voca.dto.*;
import com.d208.mr_patent_backend.domain.voca.dto.BookmarkCountDTO;
import com.d208.mr_patent_backend.domain.voca.dto.BookmarkDTO;
import com.d208.mr_patent_backend.domain.voca.dto.BookmarkResponseDTO;
import com.d208.mr_patent_backend.domain.voca.dto.bookmark.BookmarkListDTO;
import com.d208.mr_patent_backend.domain.voca.entity.Bookmark;
import com.d208.mr_patent_backend.domain.voca.entity.Word;
import com.d208.mr_patent_backend.domain.voca.repository.BookmarkRepository;
import com.d208.mr_patent_backend.domain.voca.repository.WordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookmarkService {

    private final UserRepository userRepository;
    private final WordRepository wordRepository;
    private final BookmarkRepository bookmarkRepository;

    /**
     * 북마크 추가
     */
    @Transactional
    public BookmarkResponseDTO addBookmark(String userEmail, Long wordId) {
        User user = userRepository.findByUserEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Word word = wordRepository.findById(wordId)
                .orElseThrow(() -> new IllegalArgumentException("단어를 찾을 수 없습니다."));

        // 이미 북마크가 있는지 확인
        if (bookmarkRepository.existsByUserAndWord(user, word)) {
            throw new IllegalArgumentException("이미 북마크된 단어입니다.");
        }

        // 북마크 추가
        LocalDateTime now = LocalDateTime.now();
        Bookmark bookmark = Bookmark.builder()
                .user(user)
                .word(word)
                .createdAt(now)
                .updatedAt(now)
                .build();
        bookmark = bookmarkRepository.save(bookmark);

        return BookmarkResponseDTO.builder()
                .bookmark_id(bookmark.getId())
                .build();
    }

    /**
     * 북마크 삭제
     */
    @Transactional
    public void deleteBookmark(String userEmail, Long bookmarkId) {
        User user = userRepository.findByUserEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Bookmark bookmark = bookmarkRepository.findById(bookmarkId)
                .orElseThrow(() -> new IllegalArgumentException("북마크를 찾을 수 없습니다."));

        // 본인의 북마크인지 확인
        if (!bookmark.getUser().getUserEmail().equals(user.getUserEmail())) {
            throw new IllegalArgumentException("본인의 북마크만 삭제할 수 있습니다.");
        }

        // 북마크 삭제
        bookmarkRepository.delete(bookmark);
    }
    /**
     * 레벨별 북마크 개수 조회
     */
    @Transactional(readOnly = true)
    public List<BookmarkCountDTO> getBookmarkCounts(String userEmail, Byte levelId) {
        User user = userRepository.findByUserEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        List<BookmarkCountDTO> result = new ArrayList<>();

        if (levelId != null) {
            // 특정 레벨의 북마크 개수만 조회
            long count = bookmarkRepository.countByUserAndWordLevel(user, levelId);
            result.add(BookmarkCountDTO.builder()
                    .level_id(levelId)
                    .count((int) count)
                    .build());
        } else {
            // 모든 레벨의 북마크 개수 조회
            List<Object[]> countsByLevel = bookmarkRepository.countByUserGroupByWordLevel(user);

            // 결과 변환
            for (Object[] entry : countsByLevel) {
                Byte level = (Byte) entry[0];
                Long count = (Long) entry[1];
                result.add(BookmarkCountDTO.builder()
                        .level_id(level)
                        .count(count.intValue())
                        .build());
            }
        }

        return result;
    }

    /**
     * 북마크 목록 조회
     */
    @Transactional
    public BookmarkListDTO getBookmarks(String userEmail, Byte levelId) {
        User user = userRepository.findByUserEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        List<Bookmark> bookmarks;
        if (levelId != null) {
            bookmarks = bookmarkRepository.findByUserAndWordLevelOrderByIdDesc(user, levelId);
        } else {
            bookmarks = bookmarkRepository.findByUserOrderByIdDesc(user);
        }

        List<BookmarkDTO> bookmarkDTOs = bookmarks.stream()
                .map(bookmark -> BookmarkDTO.builder()
                        .bookmark_id(bookmark.getId())
                        .word_id(bookmark.getWord().getId())
                        .word_name(bookmark.getWord().getName())
                        .word_mean(bookmark.getWord().getMean())
                        .build())
                .collect(Collectors.toList());

        return BookmarkListDTO.builder()
                .bookmarks(bookmarkDTOs)
                .total(bookmarkDTOs.size())
                .build();
    }

    /**
     * 특정 단어의 북마크 여부 확인
     */
    @Transactional
    public boolean isBookmarked(String userEmail, Long wordId) {
        User user = userRepository.findByUserEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Word word = wordRepository.findById(wordId)
                .orElseThrow(() -> new IllegalArgumentException("단어를 찾을 수 없습니다."));

        return bookmarkRepository.existsByUserAndWord(user, word);
    }
}