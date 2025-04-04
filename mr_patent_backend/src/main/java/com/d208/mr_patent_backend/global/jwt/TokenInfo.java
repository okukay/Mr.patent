package com.d208.mr_patent_backend.global.jwt;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class TokenInfo {
    private Integer userId;
    private String userEmail;
    private String userName;
    private Integer userRole;

    private String grantType;
    private String accessToken;
    private String refreshToken;
}
