package com.d208.mr_patent_backend.domain.voca.controller;

import com.d208.mr_patent_backend.domain.voca.dto.quiz.QuizDTO;
import com.d208.mr_patent_backend.domain.voca.dto.quiz.QuizResultDTO;
import com.d208.mr_patent_backend.domain.voca.dto.quiz.QuizSubmitRequest;
import com.d208.mr_patent_backend.domain.voca.service.WordQuizService;
import com.d208.mr_patent_backend.global.dto.CommonResponseDto;
import com.d208.mr_patent_backend.global.jwt.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "단어 퀴즈 API", description = "단어 퀴즈 생성 및 제출 기능")
@RestController
@RequestMapping("/api/study/levels")
@RequiredArgsConstructor
@Slf4j
public class WordQuizController {

    private final WordQuizService wordQuizService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 퀴즈 문제 조회
     */
    @GetMapping("/{level_id}/quiz")
    public ResponseEntity<CommonResponseDto<QuizDTO>> getQuiz(
            Authentication authentication,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable("level_id") Byte levelId) {

        String userEmail;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            userEmail = jwtTokenProvider.getUserEmail(token);
        } else {
            throw new RuntimeException("인증 정보를 찾을 수 없습니다.");
        }

        QuizDTO quiz = wordQuizService.generateQuiz(userEmail, levelId);
        return ResponseEntity.ok(CommonResponseDto.success(quiz));
    }

    /**
     * 퀴즈 결과 제출
     */
    @PostMapping("/{level_id}/quiz-results")
    public ResponseEntity<CommonResponseDto<QuizResultDTO>> submitQuizResult(
            Authentication authentication,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable("level_id") Byte levelId,
            @RequestBody QuizSubmitRequest request) {

        String userEmail;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            userEmail = jwtTokenProvider.getUserEmail(token);
        } else {
            throw new RuntimeException("인증 정보를 찾을 수 없습니다.");
        }

        QuizResultDTO result = wordQuizService.submitQuiz(userEmail, levelId, request);
        return ResponseEntity.ok(CommonResponseDto.success(result));
    }
}