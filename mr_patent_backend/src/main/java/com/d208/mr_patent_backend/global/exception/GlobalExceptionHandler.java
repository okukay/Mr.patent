//package com.d208.mr_patent_backend.global.exception;
//
//import com.d208.mr_patent_backend.global.dto.CommonResponseDto;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.ExceptionHandler;
//import org.springframework.web.bind.annotation.RestControllerAdvice;
//
//@RestControllerAdvice
//public class GlobalExceptionHandler {
//
//    @ExceptionHandler(BusinessException.class)
//    public ResponseEntity<CommonResponseDto<Void>> handleBusinessException(BusinessException ex) {
//        ErrorCode errorCode = ex.getErrorCode();
//        return ResponseEntity
//                .status(errorCode.getStatus())
//                .body(CommonResponseDto.error(errorCode.code(), errorCode.getMessage()));
//    }
//}