package com.d208.mr_patent_backend.domain.user.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@Getter
@Setter
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class EmailAvailableResponseDTO {
    private boolean available;

    public static EmailAvailableResponseDTO of(boolean available) {
        EmailAvailableResponseDTO response = new EmailAvailableResponseDTO();
        response.setAvailable(available);
        return response;
    }
}