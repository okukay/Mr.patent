package com.d208.mr_patent_backend.domain.user.service;

import com.d208.mr_patent_backend.domain.s3.service.S3Service;
import com.d208.mr_patent_backend.domain.user.dto.ExpertResponseDTO;
import com.d208.mr_patent_backend.domain.user.dto.ExpertDetailResponseDTO;
import com.d208.mr_patent_backend.domain.user.dto.ExpertCategoryDTO;
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
    private final S3Service s3Service;


    // 변리사 목록 조회하기
    public List<ExpertResponseDTO> getApprovedExperts() {
        List<Expert> experts = expertRepository.findByExpertStatus(1);
        return experts.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    private ExpertResponseDTO convertToDTO(Expert expert) {
        // 카테고리 이름 리스트 추출
        List<ExpertCategoryDTO> categories = expert.getExpertCategory().stream()
                .map(ec -> {
                    ExpertCategoryDTO dto = new ExpertCategoryDTO();
                    dto.setCategoryName(ec.getCategory().getCategoryName());
                    return dto;
                })
                .collect(Collectors.toList());

        String userImage = expert.getUser().getUserImage();
        String imageUrl = s3Service.generatePresignedDownloadUrl(userImage);


        return ExpertResponseDTO.builder()
                .expertId(expert.getExpertId())
                .userName(expert.getUser().getUserName())
                .userEmail(expert.getUser().getUserEmail())
                .expertDescription(expert.getExpertDescription())
                .expertAddress(expert.getExpertAddress())
                .expertPhone(expert.getExpertPhone())
                .expertGetDate(expert.getExpertGetDate())
                .expertCategories(categories)
                .userImage(imageUrl)
                .build();
    }

    // expert 상세 정보 불러오기
    public ExpertDetailResponseDTO getExpertDetail(Integer expertId) {
        Expert expert = expertRepository.findByIdWithDetails(expertId)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 변리사입니다."));

        if (expert.getExpertStatus() != 1) {
            throw new RuntimeException("승인되지 않은 변리사입니다.");
        }

        User user = expert.getUser();

        // image 다운로드 url 추출
        String presignedUrl = s3Service.generatePresignedDownloadUrl(user.getUserImage());
        System.out.println(presignedUrl);

        // 카테고리 이름 리스트 추출
        List<ExpertCategoryDTO> categories = expert.getExpertCategory().stream()
                .map(ec -> {
                    ExpertCategoryDTO dto = new ExpertCategoryDTO();
                    dto.setCategoryName(ec.getCategory().getCategoryName());
                    return dto;
                })
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
                .userImage(presignedUrl)
                .expertCategories(categories)
                .build();
    }
}