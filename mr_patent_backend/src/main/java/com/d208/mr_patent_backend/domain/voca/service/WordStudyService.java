package com.d208.mr_patent_backend.domain.voca.service;

import com.d208.mr_patent_backend.domain.user.entity.User;
import com.d208.mr_patent_backend.domain.user.repository.UserRepository;
import com.d208.mr_patent_backend.domain.voca.dto.level.LevelDTO;
import com.d208.mr_patent_backend.domain.voca.dto.level.WordDTO;
import com.d208.mr_patent_backend.domain.voca.dto.quiz.QuizDTO;
import com.d208.mr_patent_backend.domain.voca.dto.quiz.QuizResultDTO;
import com.d208.mr_patent_backend.domain.voca.dto.quiz.QuizSubmitRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 단어 학습 서비스를 관리하는 파사드 클래스
 * 각 세부 기능은 전문화된 서비스에 위임
 */
@Service
@RequiredArgsConstructor
public class WordStudyService {

    private final LevelService levelService;
    private final WordService wordService;
    private final WordQuizService wordQuizService;
    private final BookmarkService bookmarkService;
    private final UserRepository userRepository;

    /**
     * 사용자 이메일로 사용자 찾기 (공통 유틸리티 메서드)
     */
    public User findUserByEmail(String userEmail) {
        return userRepository.findByUserEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
    }

    /**
     * 사용자의 모든 레벨 정보 조회
     */
    @Transactional
    public List<LevelDTO> getUserLevels(String userEmail) {
        return levelService.getUserLevels(userEmail);
    }

    /**
     * 특정 레벨의 단어 목록 조회
     */
    @Transactional
    public List<WordDTO> getLevelWords(String userEmail, Byte levelId) {
        return wordService.getLevelWords(userEmail, levelId);
    }

    /**
     * 특정 레벨의 퀴즈 문제 생성
     */
    @Transactional
    public QuizDTO generateQuiz(String userEmail, Byte levelId) {
        return wordQuizService.generateQuiz(userEmail, levelId);
    }

    /**
     * 퀴즈 결과 제출 및 처리
     */
    @Transactional
    public QuizResultDTO submitQuiz(String userEmail, Byte levelId, QuizSubmitRequest request) {
        return wordQuizService.submitQuiz(userEmail, levelId, request);
    }
}