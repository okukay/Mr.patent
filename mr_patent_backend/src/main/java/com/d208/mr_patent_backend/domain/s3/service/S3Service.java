package com.d208.mr_patent_backend.domain.s3.service;

import com.d208.mr_patent_backend.global.config.awss3.AwsS3Config;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final AwsS3Config awsS3Config;

    public String generatePresignedUploadUrl(String fileName, String contentType) {
        S3Presigner presigner = awsS3Config.s3Presigner();

        // 어떤 객체를 올릴지 정의
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(awsS3Config.getBucket())
                .key(fileName)
                .contentType(contentType)
                .build();

        // Presigned URL 생성 설정
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(5)) // 유효 시간 설정
                .putObjectRequest(putObjectRequest)
                .build();

        // Presigned URL 생성
        PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(presignRequest);

        return presignedRequest.url().toString();
    }
}
