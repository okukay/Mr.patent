package com.d208.mr_patent_backend.domain.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import com.d208.mr_patent_backend.domain.user.dto.ExpertApproveResponseDTO;
import com.d208.mr_patent_backend.domain.user.dto.ExpertCategoryDTO;
import com.d208.mr_patent_backend.domain.user.entity.Expert;
import com.d208.mr_patent_backend.domain.user.repository.ExpertRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
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
        try {

            List<ExpertCategoryDTO> categories = expert.getExpertCategory().stream()
                    .map(ec -> {
                        ExpertCategoryDTO dto = new ExpertCategoryDTO();
                        dto.setCategoryName(ec.getCategory().getCategoryName());
                        return dto;
                    })
                    .collect(Collectors.toList());

            return ExpertApproveResponseDTO.builder()
                    .expertId(expert.getExpertId())
                    .userEmail(expert.getUser().getUserEmail())
                    .userName(expert.getUser().getUserName())
                    .expertGetDate(expert.getExpertGetDate())
                    .expertStatus(expert.getExpertStatus())
                    .expertLicense(expert.getExpertLicense())
                    .expertCreatedAt(expert.getExpertCreatedAt())
                    .expertCategories(categories)
                    .build();
        } catch (RuntimeException e) {
            throw new RuntimeException("변리사 정보 변환 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}