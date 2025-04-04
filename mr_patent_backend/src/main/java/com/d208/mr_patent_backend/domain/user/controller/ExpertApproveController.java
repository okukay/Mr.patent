package com.d208.mr_patent_backend.domain.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class ExpertApproveController {
    private final ExpertApproveService expertApproveService;

    @Operation(summary = "승인 대기 변리사 조회")
    @GetMapping
    public ResponseEntity<Map<String, Object>> getPendingExperts() {
        try {
            List<ExpertApproveResponseDTO> experts = expertApproveService.getPendingExperts();
            Map<String, Object> response = new HashMap<>();
            response.put("data", experts);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("승인 대기 변리사 조회 실패: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            Map<String, String> data = new HashMap<>();
            data.put("message", e.getMessage());
            response.put("data", data);
            return ResponseEntity.badRequest().body(response);
        }
    }

    @Operation(summary = "변리사 승인")
    @PatchMapping("/{expertId}")
    public ResponseEntity<Map<String, Object>> approveExpert(
            @PathVariable Integer expertId,
            @RequestBody ExpertApproveRequestDTO request) {
        try {
            expertApproveService.updateExpertStatus(expertId, request.getStatus());
            Map<String, Object> response = new HashMap<>();
            Map<String, String> data = new HashMap<>();
            data.put("message", "변리사 상태가 업데이트되었습니다.");
            response.put("data", data);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("변리사 승인 실패: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            Map<String, String> data = new HashMap<>();
            data.put("message", e.getMessage());
            response.put("data", data);
            return ResponseEntity.badRequest().body(response);
        }
    }
}