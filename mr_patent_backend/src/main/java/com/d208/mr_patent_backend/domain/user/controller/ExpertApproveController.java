package com.d208.mr_patent_backend.domain.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.d208.mr_patent_backend.domain.user.dto.ExpertApproveResponseDTO;
import com.d208.mr_patent_backend.domain.user.dto.ExpertApproveRequestDTO;
import com.d208.mr_patent_backend.domain.user.service.ExpertApproveService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "변리사 승인 API", description = "승인 대기 변리사 조회 및 승인")
@RestController
@RequestMapping("/api/expert-approve")
@RequiredArgsConstructor
public class ExpertApproveController {
    private final ExpertApproveService expertApproveService;

    @Operation(summary = "승인 대기 변리사 조회")
    @GetMapping
    public ResponseEntity<Map<String, Object>> getPendingExperts() {
        List<ExpertApproveResponseDTO> experts = expertApproveService.getPendingExperts();

        Map<String, Object> response = new HashMap<>();
        response.put("data", experts);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "변리사 승인")
    @PatchMapping("/{expertId}")
    public ResponseEntity<Map<String, Object>> approveExpert(
            @PathVariable Integer expertId,
            @RequestBody ExpertApproveRequestDTO request) {
        expertApproveService.updateExpertStatus(expertId, request.getStatus());

        Map<String, Object> response = new HashMap<>();
        Map<String, String> data = new HashMap<>();
        data.put("message", "변리사 상태가 업데이트되었습니다.");
        response.put("data", data);
        return ResponseEntity.ok(response);
    }
}