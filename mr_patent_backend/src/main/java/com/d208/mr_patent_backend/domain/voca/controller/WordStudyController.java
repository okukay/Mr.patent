package com.d208.mr_patent_backend.domain.voca.controller;

import com.d208.mr_patent_backend.domain.user.entity.User;
import com.d208.mr_patent_backend.domain.voca.dto.level.LevelDTO;
import com.d208.mr_patent_backend.domain.voca.dto.level.WordDTO;
import com.d208.mr_patent_backend.domain.voca.service.WordStudyService;
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

@Tag(name = "단어 학습 API", description = "단어 학습, 퀴즈, 레벨 관리 기능")
@RestController
@RequestMapping("/api/study")
@RequiredArgsConstructor
@Slf4j
public class WordStudyController {

    private final WordStudyService wordStudyService;
    private final SecurityUtils securityUtils;

    @Operation(summary = "사용자 레벨 목록 조회")
    @GetMapping("/levels")
    public ResponseEntity<CommonResponseDto<Map<String, List<LevelDTO>>>> getUserLevels(
            HttpServletRequest request) {

        User user = securityUtils.getCurrentUser(request);

        List<LevelDTO> levels = wordStudyService.getUserLevels(user.getUserEmail());
        Map<String, List<LevelDTO>> responseData = Map.of("levels", levels);
        return ResponseEntity.ok(CommonResponseDto.success(responseData));
    }

    @Operation(summary = "특정 레벨 단어 목록 조회")
    @GetMapping("/levels/{level_id}/words")
    public ResponseEntity<CommonResponseDto<Map<String, List<WordDTO>>>> getLevelWords(
            HttpServletRequest request,
            @PathVariable("level_id") Byte levelId) {

        User user = securityUtils.getCurrentUser(request);
        List<WordDTO> words = wordStudyService.getLevelWords(user.getUserEmail(), levelId);

        Map<String, List<WordDTO>> responseData = Map.of("words", words);
        return ResponseEntity.ok(CommonResponseDto.success(responseData));
    }
}