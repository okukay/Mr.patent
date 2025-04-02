package com.d208.mr_patent_backend.domain.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import com.d208.mr_patent_backend.domain.user.dto.ExpertApproveResponseDTO;
import com.d208.mr_patent_backend.domain.user.entity.Expert;
import com.d208.mr_patent_backend.domain.user.repository.ExpertRepository;

@Service
@RequiredArgsConstructor
public class ExpertApproveService {
    private final ExpertRepository expertRepository;

    public List<ExpertApproveResponseDTO> getPendingExperts() {
        return expertRepository.findByExpertStatus(0)  // 미승인 상태
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateExpertStatus(Integer expertId, Integer status) {
        Expert expert = expertRepository.findById(expertId)
                .orElseThrow(() -> new RuntimeException("변리사를 찾을 수 없습니다."));

        if (status != 0 && status != 1) {  // 0: 미승인, 1: 승인
            throw new RuntimeException("잘못된 상태값입니다.");
        }

        expert.setExpertStatus(status);
        expertRepository.save(expert);
    }

    private ExpertApproveResponseDTO convertToDTO(Expert expert) {
        return ExpertApproveResponseDTO.builder()
                .expertId(expert.getExpertId())
                .userEmail(expert.getUser().getUserEmail())
                .userName(expert.getUser().getUserName())
                .expertGetDate(expert.getExpertGetDate())
                .expertStatus(expert.getExpertStatus())
                .createdAt(expert.getExpertCreatedAt())
                .build();
    }
}