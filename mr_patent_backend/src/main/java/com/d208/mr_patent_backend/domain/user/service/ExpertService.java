package com.d208.mr_patent_backend.domain.user.service;

import com.d208.mr_patent_backend.domain.user.dto.ExpertResponseDTO;
import com.d208.mr_patent_backend.domain.user.dto.ExpertDetailResponseDTO;
import com.d208.mr_patent_backend.domain.user.entity.Expert;
import com.d208.mr_patent_backend.domain.user.repository.ExpertRepository;
import com.d208.mr_patent_backend.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExpertService {
    private final ExpertRepository expertRepository;

    public List<ExpertResponseDTO> getApprovedExperts() {
        List<Expert> experts = expertRepository.findByExpertStatus(1);
        return experts.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private ExpertResponseDTO convertToDTO(Expert expert) {
        // 카테고리 이름 리스트 추출
        List<String> categories = expert.getExpertCategory().stream()
                .map(ec -> ec.getCategory().getCategoryName())
                .collect(Collectors.toList());

        return ExpertResponseDTO.builder()
                .expertId(expert.getExpertId())
                .name(expert.getUser().getUserName())
                .email(expert.getUser().getUserEmail())
                .description(expert.getExpertDescription())
                .address(expert.getExpertAddress())
                .phone(expert.getExpertPhone())
                .license(expert.getExpertLicense())
                .getDate(expert.getExpertGetDate())
                .categories(categories)
                .build();
    }

    public ExpertDetailResponseDTO getExpertDetail(Integer expertId) {
        Expert expert = expertRepository.findByIdWithDetails(expertId)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 변리사입니다."));

        if (expert.getExpertStatus() != 1) {
            throw new RuntimeException("승인되지 않은 변리사입니다.");
        }

        User user = expert.getUser();

        // 카테고리 이름 리스트 추출
        List<String> categories = expert.getExpertCategory().stream()
                .map(ec -> ec.getCategory().getCategoryName())
                .collect(Collectors.toList());

        return ExpertDetailResponseDTO.builder()
                .expertId(expert.getExpertId())
                .userId(user.getUserId())
                .userName(user.getUserName())
                .expertDescription(expert.getExpertDescription())
                .expertAddress(expert.getExpertAddress())
                .expertPhone(expert.getExpertPhone())
                .expertGetDate(expert.getExpertGetDate())
                .expertCreatedAt(expert.getExpertCreatedAt())
                .userEmail(user.getUserEmail())
                .userImage(user.getUserImage())
                .categories(categories)
                .build();
    }
}