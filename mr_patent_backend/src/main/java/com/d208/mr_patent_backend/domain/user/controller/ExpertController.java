package com.d208.mr_patent_backend.domain.user.controller;

import com.d208.mr_patent_backend.domain.user.dto.ExpertResponseDTO;
import com.d208.mr_patent_backend.domain.user.dto.ExpertDetailResponseDTO;
import com.d208.mr_patent_backend.domain.user.service.ExpertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "변리사 API", description = "변리사 조회")
@RestController
@RequestMapping("/api/expert")
@RequiredArgsConstructor
public class ExpertController {
    private final ExpertService expertService;

    @Operation(summary = "변리사 리스트 조회")
    @GetMapping
    public ResponseEntity<Map<String, Object>> getApprovedExperts() {
        List<ExpertResponseDTO> experts = expertService.getApprovedExperts();

        Map<String, Object> response = new HashMap<>();
        response.put("data", experts);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "변리사 상세 조회")
    @GetMapping("/{expertId}")
    public ResponseEntity<Map<String, Object>> getExpertDetail(
            @PathVariable Integer expertId) {
        ExpertDetailResponseDTO expert = expertService.getExpertDetail(expertId);

        Map<String, Object> response = new HashMap<>();
        response.put("data", expert);

        return ResponseEntity.ok(response);
    }
}