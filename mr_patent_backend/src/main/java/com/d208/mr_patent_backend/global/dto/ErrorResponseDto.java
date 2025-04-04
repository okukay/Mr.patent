package com.d208.mr_patent_backend.global.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 에러 응답 정보를 담는 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponseDto {
    private String code;
    private String message;
}


//package com.d208.mr_patent_backend.global.dto;
//
//import com.d208.mr_patent_backend.global.exception.ErrorCode;
//import lombok.AllArgsConstructor;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//
///**
// * 에러 응답 정보를 담는 DTO
// */
//@Data
//@NoArgsConstructor
//@AllArgsConstructor
//public class ErrorResponseDto {
//    private String code;
//    private String message;
//
//    public static ErrorResponseDto of(ErrorCode errorCode) {
//        return new ErrorResponseDto(errorCode.code(), errorCode.getMessage());
//    }
//}

//// 1
//@GetMapping("")
//public ResponseEntity<CommonResponseDto<BookmarkListDTO>> getBookmarks(...) {
//    BookmarkListDTO bookmarks = bookmarkService.getBookmarks(user.getUserEmail(), levelId);
//    return ResponseEntity.ok(CommonResponseDto.success(bookmarks));
//}
//
//// 2
//@GetMapping("")
//public ResponseEntity<Map<String, Object>> getBookmarks(...) {
//    BookmarkListDTO bookmarks = bookmarkService.getBookmarks(user.getUserEmail(), levelId);
//    Map<String, Object> response = new HashMap<>();
//    response.put("data", bookmarks);
//    return ResponseEntity.ok(response);
//}