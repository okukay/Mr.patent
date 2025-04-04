package com.d208.mr_patent_backend.domain.voca.service;

import com.d208.mr_patent_backend.domain.user.repository.UserRepository;
import com.d208.mr_patent_backend.domain.voca.dto.LevelDTO;
import com.d208.mr_patent_backend.domain.voca.dto.level.LevelWordsDTO;
import com.d208.mr_patent_backend.domain.voca.dto.quiz.QuizDTO;
import com.d208.mr_patent_backend.domain.voca.dto.quiz.QuizResultDTO;
import com.d208.mr_patent_backend.domain.voca.dto.quiz.QuizSubmitRequest;
import com.d208.mr_patent_backend.domain.voca.repository.BookmarkRepository;
import com.d208.mr_patent_backend.domain.voca.repository.QuizResultRepository;
import com.d208.mr_patent_backend.domain.voca.repository.UserLevelRepository;
import com.d208.mr_patent_backend.domain.voca.repository.WordRepository;
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
    private final WordQuizService wordquizService;

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
    public LevelWordsDTO getLevelWords(String userEmail, Byte levelId) {
        return wordService.getLevelWords(userEmail, levelId);
    }

    /**
     * 특정 레벨의 퀴즈 문제 생성
     */
    @Transactional
    public QuizDTO generateQuiz(String userEmail, Byte levelId) {
        return wordquizService.generateQuiz(userEmail, levelId);
    }

    /**
     * 퀴즈 결과 제출 및 처리
     */
    @Transactional
    public QuizResultDTO submitQuiz(String userEmail, Byte levelId, QuizSubmitRequest request) {
        return wordquizService.submitQuiz(userEmail, levelId, request);
    }
}