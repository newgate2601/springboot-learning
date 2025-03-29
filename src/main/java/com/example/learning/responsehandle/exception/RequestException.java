package com.example.learning.responsehandle.exception;

import com.example.learning.responsehandle.error.ErrorCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class RequestException extends RuntimeException {
    private Integer status;
    private ErrorCode errorCode;

    public RequestException(Integer status, ErrorCode errorCode) {
        this.status = status;
        this.errorCode = errorCode;
    }

    public RequestException(ErrorCode errorCode) {
        this.status = 400;
        this.errorCode = errorCode;
    }
}
