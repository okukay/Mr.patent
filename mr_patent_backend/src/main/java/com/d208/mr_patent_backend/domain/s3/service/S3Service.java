package com.d208.mr_patent_backend.domain.s3.service;

import com.d208.mr_patent_backend.global.config.awss3.AwsS3Config;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class S3Service {

    //accessKey, secretKey, region 정보들
    private final AwsS3Config awsS3Config;

    //파일 업로드용 Presigned URL 생성
    public String generatePresignedUploadUrl(String fileName, String contentType) {
        S3Presigner presigner = awsS3Config.s3Presigner();

        // 어떤 객체를 올릴지 정의
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(awsS3Config.getS3().getBucket()) // 업로드할 S3 버킷 이름
                .key(fileName)                   // s3에 저장될 경로 + 이름
                .contentType(contentType)        // MIME 타입
                .build();

        // Presigned URL 설정
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(15)) // 유효 시간 5분
                .putObjectRequest(putObjectRequest)       // putObjectRequest 이 객체를 업로드 하겠다 ~
                .build();

        // Presigned URL 생성
        PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(presignRequest);
        return presignedRequest.url().toString();
    }



    //파일 다운로드용 Presigned URL 생성
    public String generatePresignedDownloadUrl(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return null;
        }

        S3Presigner presigner = awsS3Config.s3Presigner();

        // 다운로드할 파일 정보
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(awsS3Config.getS3().getBucket())
                .key(fileName)
                .build();

        // Presigned GET URL 생성 설정
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(15)) // 유효 시간 설정 (일회성, 시간제한된 접근 링크라 없으면 url 생성 안댐)
                .getObjectRequest(getObjectRequest)
                .build();

        // Presigned URL 생성
        PresignedGetObjectRequest presignedRequest = presigner.presignGetObject(presignRequest);

        return presignedRequest.url().toString();
    }

}
