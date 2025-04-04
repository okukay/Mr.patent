package com.d208.mr_patent_backend.domain.voca.controller;

import com.d208.mr_patent_backend.domain.user.entity.User;
import com.d208.mr_patent_backend.domain.voca.dto.quiz.QuizDTO;
import com.d208.mr_patent_backend.domain.voca.dto.quiz.QuizResultDTO;
import com.d208.mr_patent_backend.domain.voca.dto.quiz.QuizSubmitRequest;
import com.d208.mr_patent_backend.domain.voca.service.WordQuizService;
import com.d208.mr_patent_backend.global.dto.CommonResponseDto;
import com.d208.mr_patent_backend.global.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "단어 퀴즈 API", description = "단어 퀴즈 생성 및 제출 기능")
@RestController
@RequestMapping("/api/study/levels")
@RequiredArgsConstructor
@Slf4j
public class WordQuizController {

    private final WordQuizService wordQuizService;
    private final SecurityUtils securityUtils;

    /**
     * 퀴즈 문제 조회
     */
    @Operation(summary = "퀴즈 문제 생성")
    @GetMapping("/{level_id}/quiz")
    public ResponseEntity<CommonResponseDto<QuizDTO>> getQuiz(
            HttpServletRequest request,
            @PathVariable("level_id") Byte levelId) {

        User user = securityUtils.getCurrentUser(request);

        QuizDTO quiz = wordQuizService.generateQuiz(user.getUserEmail(), levelId);
        return ResponseEntity.ok(CommonResponseDto.success(quiz));
    }

    /**
     * 퀴즈 결과 제출
     */
    @Operation(summary = "퀴즈 결과 제출")
    @PostMapping("/{level_id}/quiz-results")
    public ResponseEntity<CommonResponseDto<QuizResultDTO>> submitQuizResult(
            HttpServletRequest request,
            @PathVariable("level_id") Byte levelId,
            @RequestBody QuizSubmitRequest submitRequest) {

        User user = securityUtils.getCurrentUser(request);

        QuizResultDTO result = wordQuizService.submitQuiz(user.getUserEmail(), levelId, submitRequest);
        return ResponseEntity.ok(CommonResponseDto.success(result));
    }
}