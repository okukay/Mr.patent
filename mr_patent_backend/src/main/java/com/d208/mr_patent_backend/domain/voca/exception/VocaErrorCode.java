//package com.d208.mr_patent_backend.domain.voca.exception;
//
//import com.d208.mr_patent_backend.global.exception.ErrorCode;
//import lombok.Getter;
//import lombok.RequiredArgsConstructor;
//
//@Getter
//@RequiredArgsConstructor
//public enum VocaErrorCode implements ErrorCode {
//    // 레벨 관련 에러
//    LEVEL_NOT_ACCESSIBLE(403, "이전 레벨을 먼저 통과해야 합니다."),
//    LEVEL_NOT_FOUND(404, "해당 레벨을 찾을 수 없습니다."),
//
//    // 단어 관련 에러
//    WORD_NOT_FOUND(404, "단어를 찾을 수 없습니다."),
//
//    // 북마크 관련 에러
//    BOOKMARK_CREATION_FAILED(500, "북마크 생성에 실패했습니다."),
//    BOOKMARK_DELETION_FAILED(500, "북마크 삭제에 실패했습니다.");
//
//    private final int status;
//    private final String message;
//
//    @Override
//    public int getStatus() {
//        return status;
//    }
//
//    @Override
//    public String getMessage() {
//        return message;
//    }
//
//    @Override
//    public String code() {
//        return name();
//    }
//}