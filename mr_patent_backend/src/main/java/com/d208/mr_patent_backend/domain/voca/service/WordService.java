package com.d208.mr_patent_backend.domain.voca.service;

import com.d208.mr_patent_backend.domain.user.entity.User;
import com.d208.mr_patent_backend.domain.voca.dto.level.WordDTO;
import com.d208.mr_patent_backend.domain.voca.entity.Bookmark;
import com.d208.mr_patent_backend.domain.voca.entity.Word;
import com.d208.mr_patent_backend.domain.voca.repository.BookmarkRepository;
import com.d208.mr_patent_backend.domain.voca.repository.WordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WordService {

    private final WordRepository wordRepository;
    private final BookmarkRepository bookmarkRepository;
    private final LevelService levelService;

    /**
     * 특정 레벨의 단어 목록 조회
     */
    @Transactional
    public List<WordDTO> getLevelWords(String userEmail, Byte levelId) {
        User user = levelService.findUserByEmail(userEmail);

        // 레벨 접근 가능 여부 확인
        if (!levelService.isLevelAccessible(user, levelId)) {
            throw new RuntimeException("이전 레벨을 먼저 통과해야 합니다.");
        }

        // 해당 레벨이 없으면 생성
        levelService.getOrCreateUserLevel(user, levelId);

        // 레벨에 해당하는 단어 조회
        List<Word> words = wordRepository.findByLevelOrderByWordIdAsc(levelId);

        // 단어 목록 변환 및 반환
        return convertToWordDTOList(words, user);
    }

    /**
     * Word 엔티티 목록을 WordDTO 목록으로 변환
     */
    private List<WordDTO> convertToWordDTOList(List<Word> words, User user) {
        return words.stream()
                .map(word -> {
                    // 북마크 조회
                    Bookmark bookmark = bookmarkRepository.findByUserAndWord(user, word);
                    boolean isBookmarked = bookmark != null;

                    return WordDTO.builder()
                            .word_id(word.getId())
                            .word_name(word.getName())
                            .word_mean(word.getMean())
                            .is_bookmarked(isBookmarked)
                            .bookmark_id(isBookmarked ? bookmark.getId() : null)
                            .build();
                })
                .collect(Collectors.toList());
    }
}