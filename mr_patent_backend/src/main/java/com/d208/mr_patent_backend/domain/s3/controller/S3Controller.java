package com.d208.mr_patent_backend.domain.s3.controller;

import com.d208.mr_patent_backend.domain.s3.service.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/s3")
public class S3Controller {

    private final S3Service s3Service;

    // 업로드용 Presigned URL 발급
    @GetMapping("/upload-url")
    public ResponseEntity<Map<String, Object>> getPresignedUploadUrl(
            @RequestParam String filename,
            @RequestParam String contenttype
    ) {
        String presignedUrl = s3Service.generatePresignedUploadUrl(filename, contenttype);
        Map<String, Object> response = new HashMap<>();
        response.put("data", presignedUrl);

        return ResponseEntity.ok(response);
    }

    // 다운로드용 Presigned URL 발급
//    @GetMapping("/download-url")
//    public ResponseEntity<Map<String, Object>> getPresignedDownloadUrl(
//            @RequestParam String filename
//    ) {
//        String presignedUrl = s3Service.generatePresignedDownloadUrl(filename);
//        Map<String, Object> response = new HashMap<>();
//        response.put("data", presignedUrl);
//
//        return ResponseEntity.ok(response);
//    }
}