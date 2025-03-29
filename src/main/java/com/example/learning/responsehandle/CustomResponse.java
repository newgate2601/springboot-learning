package com.example.learning.responsehandle;

import com.example.learning.responsehandle.error.ErrorResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomResponse<T> {
    private Integer status;
    private List<ErrorResponse> errors;
    private T body;

    public static <T> CustomResponse ok(T body) {
        return CustomResponse.builder()
                .status(200)
                .body(body)
                .build();
    }
}
