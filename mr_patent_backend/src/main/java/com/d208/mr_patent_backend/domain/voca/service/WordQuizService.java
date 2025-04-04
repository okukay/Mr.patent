package com.d208.mr_patent_backend.domain.voca.service;

import com.d208.mr_patent_backend.domain.user.entity.User;
import com.d208.mr_patent_backend.domain.voca.dto.quiz.*;
import com.d208.mr_patent_backend.domain.voca.entity.UserLevel;
import com.d208.mr_patent_backend.domain.voca.entity.Word;
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
    private final LevelService levelService;

    private static final int QUIZ_COUNT = 10;    // 퀴즈 문제 개수
    private static final int OPTIONS_COUNT = 4;  // 선택지 개수
    private static final byte PASS_SCORE = 8;    // 통과 점수 (10점 만점 중)

    /**
     * 특정 레벨의 퀴즈 문제 생성
     */
    @Transactional
    public QuizDTO generateQuiz(String userEmail, Byte levelId) {
        User user = levelService.findUserByEmail(userEmail);

        // 레벨 접근 가능 여부 확인
        if (!levelService.isLevelAccessible(user, levelId)) {
            throw new IllegalArgumentException("이전 레벨을 먼저 통과해야 합니다.");
        }

        // 해당 레벨의 단어 모두 가져오기
        List<Word> words = wordRepository.findByLevelOrderByNameAsc(levelId);
        if (words.size() < QUIZ_COUNT) {
            throw new IllegalArgumentException("해당 레벨의 단어가 충분하지 않습니다.");
        }

        // 문제 생성
        List<QuestionDTO> questions = createQuizQuestions(words);

        return QuizDTO.builder()
                .level_id(levelId)
                .questions(questions)
                .build();
    }

    /**
     * 퀴즈 문제 생성
     */
    private List<QuestionDTO> createQuizQuestions(List<Word> words) {
        // 단어 섞기
        List<Word> shuffledWords = new ArrayList<>(words);
        Collections.shuffle(shuffledWords);

        // 퀴즈 문제로 사용할 단어 선택
        List<Word> quizWords = shuffledWords.subList(0, QUIZ_COUNT);

        // 문제 생성
        List<QuestionDTO> questions = new ArrayList<>();
        Long questionId = 1L;

        for (Word word : quizWords) {
            questions.add(createQuestion(questionId++, word, words));
        }

        return questions;
    }

    /**
     * 개별 퀴즈 문제 생성
     */
    private QuestionDTO createQuestion(Long questionId, Word word, List<Word> allWords) {
        // 현재 단어를 제외한 단어들에서 오답 선택지 생성
        List<Word> otherWords = allWords.stream()
                .filter(w -> !w.getId().equals(word.getId()))
                .collect(Collectors.toList());
        Collections.shuffle(otherWords);

        // 옵션 생성
        List<OptionDTO> options = createQuestionOptions(word, otherWords);

        // 선택지 순서 섞기
        Collections.shuffle(options);

        // 정답 인덱스 찾기
        Long correctOption = options.stream()
                .filter(opt -> opt.getOption_text().equals(word.getName()))
                .findFirst()
                .map(OptionDTO::getOption_id)
                .orElse(1L);

        // 퀴즈 문제 생성 (뜻을 주고 단어 맞추기)
        return QuestionDTO.builder()
                .question_id(questionId)
                .question_text(word.getMean())
                .options(options)
                .correct_option(correctOption)
                .build();
    }

    /**
     * 퀴즈 문제 선택지 생성
     */
    private List<OptionDTO> createQuestionOptions(Word correctWord, List<Word> otherWords) {
        List<OptionDTO> options = new ArrayList<>();
        Long correctOptionId = 1L;

        // 정답 추가
        options.add(OptionDTO.builder()
                .option_id(correctOptionId)
                .option_text(correctWord.getName())
                .build());

        // 오답 선택지 추가
        for (int i = 0; i < Math.min(OPTIONS_COUNT - 1, otherWords.size()); i++) {
            options.add(OptionDTO.builder()
                    .option_id((long)(i + 2))
                    .option_text(otherWords.get(i).getName())
                    .build());
        }

        return options;
    }

    /**
     * 퀴즈 결과 제출 및 처리
     */
    @Transactional
    public QuizResultDTO submitQuiz(String userEmail, Byte levelId, QuizSubmitRequest request) {
        User user = levelService.findUserByEmail(userEmail);

        // 사용자 레벨 확인
        UserLevel userLevel = levelService.getOrCreateUserLevel(user, levelId);

        // 퀴즈 생성 (또는 캐싱된 퀴즈 가져오기)
        QuizDTO quiz = generateQuiz(userEmail, levelId);

        // 정답 체크 및 점수
        int correctCount = evaluateQuizAnswers(request.getAnswers(), quiz.getQuestions());
        byte score = calculateScore(correctCount, request.getAnswers().size());

        // 통과 여부 결정 (8점 이상)
        boolean passed = score >= PASS_SCORE;

        // 최고 점수 갱신
        if (score > userLevel.getBestScore()) {
            userLevel.updateScore(score);
        }

        // 퀴즈 통과 시 레벨 통과 처리
        if (passed) {
            levelService.processLevelPass(user, levelId, userLevel);
        }

        // 틀린 문제 목록 생성
        List<WrongAnswerDTO> wrongAnswers = createWrongAnswersList(request.getAnswers(), quiz.getQuestions());

        return QuizResultDTO.builder()
                .level_id(levelId)
                .score(score)
                .wrong_answers(wrongAnswers)
                .build();
    }

    /**
     * 퀴즈 답안 평가
     */
    private int evaluateQuizAnswers(List<QuizSubmitRequest.AnswerDTO> userAnswers, List<QuestionDTO> questions) {
        int correctCount = 0;

        // 사용자 답안을 순회하며 정답 여부 체크
        for (QuizSubmitRequest.AnswerDTO userAnswer : userAnswers) {
            // 해당 문제 찾기
            Optional<QuestionDTO> questionOpt = questions.stream()
                    .filter(q -> q.getQuestion_id().equals(userAnswer.getQuestion_id()))
                    .findFirst();

            if (questionOpt.isPresent()) {
                QuestionDTO question = questionOpt.get();
                // 사용자가 선택한 답안이 정답인지 확인
                if (question.getCorrect_option().equals(userAnswer.getSelected_option_id())) {
                    correctCount++;
                }
            }
        }

        return correctCount;
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
    private List<WrongAnswerDTO> createWrongAnswersList(List<QuizSubmitRequest.AnswerDTO> userAnswers, List<QuestionDTO> questions) {
        List<WrongAnswerDTO> wrongAnswers = new ArrayList<>();

        for (QuizSubmitRequest.AnswerDTO userAnswer : userAnswers) {
            // 해당 문제 찾기
            Optional<QuestionDTO> questionOpt = questions.stream()
                    .filter(q -> q.getQuestion_id().equals(userAnswer.getQuestion_id()))
                    .findFirst();

            if (questionOpt.isPresent()) {
                QuestionDTO question = questionOpt.get();

                // 사용자 답안이 오답인 경우
                if (!question.getCorrect_option().equals(userAnswer.getSelected_option_id())) {
                    // 정답 옵션 텍스트 찾기
                    String correctOptionText = question.getOptions().stream()
                            .filter(opt -> opt.getOption_id().equals(question.getCorrect_option()))
                            .findFirst()
                            .map(OptionDTO::getOption_text)
                            .orElse("");

                    // 틀린 문제 추가
                    wrongAnswers.add(WrongAnswerDTO.builder()
                            .question_id(question.getQuestion_id())
                            .question_text(question.getQuestion_text())
                            .correct_option_text(correctOptionText)
                            .build());
                }
            }
        }

        return wrongAnswers;
    }
}