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

    private final AwsS3Config awsS3Config;

    //파일 업로드용 Presigned URL 생성
    public String generatePresignedUploadUrl(String fileName, String contentType) {
        S3Presigner presigner = awsS3Config.s3Presigner();

        // 어떤 객체를 올릴지 정의
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(awsS3Config.getBucket()) //업로드할 S3 버킷 이름
                .key(fileName)                   // 저장할 S3 객체의 경로 (파일 이름)
                .contentType(contentType)        // MIME 타입 (중요!)
                .build();

        // Presigned URL 설정
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(5)) // 유효 시간 5분
                .putObjectRequest(putObjectRequest)
                .build();

        // Presigned URL 생성
        PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(presignRequest);

        return presignedRequest.url().toString();
    }



    //파일 다운로드용 Presigned URL 생성
    public String generatePresignedDownloadUrl(String fileName) {
        S3Presigner presigner = awsS3Config.s3Presigner();

        // 다운로드할 파일 정보
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(awsS3Config.getBucket())
                .key(fileName)
                .build();

        // Presigned GET URL 생성 설정
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(5)) // 유효 시간 설정
                .getObjectRequest(getObjectRequest)
                .build();

        // Presigned URL 생성
        PresignedGetObjectRequest presignedRequest = presigner.presignGetObject(presignRequest);

        return presignedRequest.url().toString();
    }

}
