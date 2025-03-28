package com.d208.mr_patent_backend.domain.s3.controller;

import com.d208.mr_patent_backend.domain.s3.service.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/s3")
public class S3Controller {

    private final S3Service s3Service;

    // 업로드용 Presigned URL 발급
    @GetMapping("/presigned-url")
    public ResponseEntity<String> getPresignedUploadUrl(
            @RequestParam String fileName,
            @RequestParam String contentType
    ) {
        String presignedUrl = s3Service.generatePresignedUploadUrl(fileName, contentType);
        return ResponseEntity.ok(presignedUrl);
    }
}