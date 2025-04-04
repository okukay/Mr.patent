package com.d208.mr_patent_backend.global.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 공통 응답 DTO
 * @param <T> 응답 데이터의 타입
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommonResponseDto<T> {
    private T data;
    private ErrorResponse error;

    /**
     * 성공 응답 생성
     * @param data 응답 데이터
     * @return 성공 응답 DTO
     */
    public static <T> CommonResponseDto<T> success(T data) {
        return CommonResponseDto.<T>builder()
                .data(data)
                .error(null)
                .build();
    }

    /**
     * 에러 응답 생성
     * @param code 에러 코드
     * @param message 에러 메시지
     * @return 에러 응답 DTO
     */
    public static <T> CommonResponseDto<T> error(String code, String message) {
        return CommonResponseDto.<T>builder()
                .data(null)
                .error(new ErrorResponse(code, message))
                .build();
    }
}