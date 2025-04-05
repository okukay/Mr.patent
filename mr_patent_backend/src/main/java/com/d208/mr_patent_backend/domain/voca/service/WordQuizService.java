package com.d208.mr_patent_backend.domain.voca.service;

import com.d208.mr_patent_backend.domain.user.entity.User;
import com.d208.mr_patent_backend.domain.voca.dto.level.WordDTO;
import com.d208.mr_patent_backend.domain.voca.dto.quiz.*;
import com.d208.mr_patent_backend.domain.voca.entity.Bookmark;
import com.d208.mr_patent_backend.domain.voca.entity.UserLevel;
import com.d208.mr_patent_backend.domain.voca.entity.Word;
import com.d208.mr_patent_backend.domain.voca.repository.BookmarkRepository;
import com.d208.mr_patent_backend.domain.voca.repository.UserLevelRepository;
import com.d208.mr_patent_backend.domain.voca.repository.WordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WordQuizService {
    private final WordRepository wordRepository;
    private final BookmarkRepository bookmarkRepository;
    private final UserLevelRepository userLevelRepository;
    private final LevelService levelService;

    private static final int QUIZ_COUNT = 10;
    private static final int OPTIONS_COUNT = 4;
    private static final byte PASS_SCORE = 8;

    // ---------- 퀴즈 생성 관련 메소드 ----------

    /**
     * 퀴즈 생성
     */
    @Transactional
    public QuizDTO generateQuiz(String userEmail, Byte levelId) {
        User user = levelService.findUserByEmail(userEmail);
        validateLevelAccess(user, levelId);

        List<Word> levelWords = fetchLevelWords(levelId);
        Collections.shuffle(levelWords);
        List<Word> quizWords = levelWords.subList(0, QUIZ_COUNT);

        List<QuestionDTO> questions = quizWords.stream()
                .map(word -> createQuestion(word, levelWords))
                .collect(Collectors.toList());

        return QuizDTO.builder()
                .level_id(levelId)
                .questions(questions)
                .build();
    }

    /**
     * 레벨 접근 권한 검증
     */
    private void validateLevelAccess(User user, Byte levelId) {
        if (!levelService.isLevelAccessible(user, levelId)) {
            throw new IllegalArgumentException("이전 레벨을 먼저 통과해야 합니다.");
        }
    }

    /**
     * 특정 레벨의 단어 모두 가져오기
     */
    private List<Word> fetchLevelWords(Byte levelId) {
        List<Word> levelWords = wordRepository.findByLevelOrderByNameAsc(levelId);

        if (levelWords.size() < QUIZ_COUNT) {
            throw new IllegalArgumentException("퀴즈 문제를 생성할 " + levelId + "레벨의 단어가 부족합니다.");
        }

        return levelWords;
    }

    /**
     * 퀴즈 문제 생성
     */
    private List<QuestionDTO> createQuizQuestions(List<Word> allWords) {
        Collections.shuffle(allWords);
        List<Word> quizWords = allWords.subList(0, QUIZ_COUNT);

        return quizWords.stream()
                .map(word -> createQuestion(word, allWords))
                .collect(Collectors.toList());
    }

    /**
     * 개별 퀴즈 문제 생성
     */
    private QuestionDTO createQuestion(Word word, List<Word> levelWords) {
        List<OptionDTO> options = createQuestionOptions(word, levelWords);
        Long correctOption = findCorrectOptionId(options, word);

        return QuestionDTO.builder()
                .word_id(word.getId())
                .question_text(word.getMean())
                .options(options)
                .correct_option(correctOption)
                .build();
    }

    /**
     * 문제 선택지 생성
     */
    private List<OptionDTO> createQuestionOptions(Word correctWord, List<Word> levelWords) {
        List<OptionDTO> options = new ArrayList<>();

        // 정답 추가 (임시 ID 설정)
        options.add(OptionDTO.builder()
                .option_id(0L)
                .option_text(correctWord.getName())
                .build());

        // 오답 선택지 추가 (같은 레벨의 다른 단어들)
        List<Word> otherWords = levelWords.stream()
                .filter(w -> !w.getId().equals(correctWord.getId()))
                .collect(Collectors.toList());
        Collections.shuffle(otherWords);

        for (int i = 0; i < Math.min(OPTIONS_COUNT - 1, otherWords.size()); i++) {
            options.add(OptionDTO.builder()
                    .option_id(0L)
                    .option_text(otherWords.get(i).getName())
                    .build());
        }

        // 옵션 섞기
        Collections.shuffle(options);

        // 섞은 후 순차적으로 ID 부여
        for (int i = 0; i < options.size(); i++) {
            options.get(i).setOption_id((long) (i + 1));
        }

        return options;
    }

    /**
     * 정답 옵션 ID 찾기
     */
    private Long findCorrectOptionId(List<OptionDTO> options, Word correctWord) {
        return options.stream()
                .filter(opt -> opt.getOption_text().equals(correctWord.getName()))
                .findFirst()
                .map(OptionDTO::getOption_id)
                .orElse(options.get(0).getOption_id());
    }

    // ---------- 퀴즈 결과 처리 관련 메소드 ----------

    /**
     * 퀴즈 결과 제출 및 처리
     */
    @Transactional
    public QuizResultDTO submitQuiz(String userEmail, Byte levelId, QuizSubmitRequest request) {
        User user = levelService.findUserByEmail(userEmail);
        UserLevel userLevel = levelService.getOrCreateUserLevel(user, levelId);

        // 틀린 문제들의 단어 ID 목록
        List<Long> wrongWordIds = request.getAnswers().stream()
                .map(QuizSubmitRequest.AnswerDTO::getWord_id)
                .collect(Collectors.toList());

        // 점수 계산 및 업데이트
        byte score = processQuizScore(userLevel, wrongWordIds.size(), QUIZ_COUNT, levelId, user);

        // 틀린 문제 정보 생성 (북마크 정보 포함)
        List<WrongAnswerDTO> wrongAnswers = createWrongAnswersList(wrongWordIds, user);

        return QuizResultDTO.builder()
                .level_id(levelId)
                .score(score)
                .wrong_answers(wrongAnswers)
                .build();
    }


    /**
     * 퀴즈 단어 ID 목록 가져오기
     */
    private List<Long> getQuizWordIds(String userEmail, Byte levelId) {
        QuizDTO quiz = generateQuiz(userEmail, levelId);
        return quiz.getQuestions().stream()
                .map(QuestionDTO::getWord_id)
                .collect(Collectors.toList());
    }

    /**
     * 퀴즈 점수 처리 및 레벨 업데이트
     */
    private byte processQuizScore(UserLevel userLevel, int wrongCount, int totalQuestions, Byte levelId, User user) {
        int correctCount = totalQuestions - wrongCount;
        byte score = calculateScore(correctCount, totalQuestions);

        System.out.println("현재 점수: " + score + ", 기존 점수: " + userLevel.getBestScore());

        // 처음 퀴즈를 풀거나(bestScore가 100인 경우) 더 높은 점수를 얻었을 때만 업데이트
        if (userLevel.getBestScore() == 100 || (score > userLevel.getBestScore() && score <= 10)) {
            userLevel.updateScore(score);
            userLevelRepository.save(userLevel);
            System.out.println("점수 업데이트: " + score);
        }

        // 통과 여부 처리 (8점 이상이고 아직 통과하지 않은 경우)
        if (score >= PASS_SCORE && userLevel.getIsPassed() == 0) {
            levelService.processLevelPass(user, levelId, userLevel);
            System.out.println("레벨 " + levelId + " 통과 처리 완료");
        }

        return score;
    }

    /**
     * 점수 계산 (10점 만점)
     */
    private byte calculateScore(int correctCount, int totalQuestions) {
        return (byte) Math.round((double) correctCount / totalQuestions * 10);
    }

    /**
     * 틀린 문제 목록 생성
     */
    private List<WrongAnswerDTO> createWrongAnswersList(List<Long> wrongWordIds, User user) {
        return wrongWordIds.stream()
                .map(wordId -> {
                    Word word = wordRepository.findById(wordId)
                            .orElseThrow(() -> new IllegalArgumentException("단어를 찾을 수 없습니다."));

                    // 북마크 정보 조회
                    Bookmark bookmark = bookmarkRepository.findByUserAndWord(user, word);
                    boolean isBookmarked = bookmark != null;

                    return WrongAnswerDTO.builder()
                            .word_id(wordId)
                            .question_text(word.getMean())
                            .correct_option_text(word.getName())
                            .bookmarked(isBookmarked)
                            .bookmark_id(isBookmarked ? bookmark.getId() : null)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 단어 정보 목록 생성
     */
    private List<WordDTO> createWordDTOList(List<Long> wordIds, User user) {
        return wordIds.stream()
                .map(wordId -> {
                    Word word = wordRepository.findById(wordId)
                            .orElseThrow(() -> new IllegalArgumentException("단어를 찾을 수 없습니다."));

                    Bookmark bookmark = bookmarkRepository.findByUserAndWord(user, word);
                    boolean isBookmarked = bookmark != null;

                    return WordDTO.builder()
                            .word_id(word.getId())
                            .word_name(word.getName())
                            .word_mean(word.getMean())
                            .bookmarked(isBookmarked)
                            .bookmark_id(isBookmarked ? bookmark.getId() : null)
                            .build();
                })
                .collect(Collectors.toList());
    }
}