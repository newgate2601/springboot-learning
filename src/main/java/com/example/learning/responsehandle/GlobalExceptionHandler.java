package com.example.learning.responsehandle;

import com.example.learning.responsehandle.error.ErrorCode;
import com.example.learning.responsehandle.error.ErrorResponse;
import com.example.learning.responsehandle.exception.RequestException;
import com.example.learning.responsehandle.exception.TestException;
import com.example.learning.responsehandle.message.MessageService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.*;

@RestControllerAdvice
@AllArgsConstructor
public class GlobalExceptionHandler {
    private final MessageService messageService;

    // Xử lý lỗi validation của @Valid trong request body
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    public Map<String, String> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        return errors;
    }

    // Xử lý lỗi validation của @RequestParam, @PathVariable, @Valid trên các tham số method
//    @ExceptionHandler(ConstraintViolationException.class)
//    @ResponseBody
//    public Map<String, String> handleConstraintViolation(ConstraintViolationException ex) {
//        Map<String, String> errors = new HashMap<>();
//        ex.getConstraintViolations().forEach(violation ->
//                errors.put(violation.getPropertyPath().toString(), violation.getMessage())
//        );
//        return errors;
//    }

    @ExceptionHandler({TestException.class})
    public ResponseEntity<String> handleTestException(TestException e) {
        return ResponseEntity.status(432).body(e.getMessage());
    }

    @ExceptionHandler({RequestException.class}) // ResponseEntity<CustomResponse>
    public ResponseEntity<CustomResponse> handleRequestException(RequestException e) {
        ErrorCode errorCode = e.getErrorCode();
//        return CustomResponse.builder()
//                .status(e.getStatus())
//                .errors(getErrorResponses(List.of(errorCode)))
//                .build();
        return ResponseEntity.status(400).body(
                CustomResponse.builder()
                        .status(e.getStatus())
                        .errors(getErrorResponses(List.of(errorCode)))
                        .build()
        );
    }

    private List<ErrorResponse> getErrorResponses(List<ErrorCode> errorCodes) {
        String lang = getHeaderValue("Accept-Language");
        Locale locale = (lang != null && lang.equals("en")) ? Locale.ENGLISH : new Locale("vi");
        List<ErrorResponse> errorResponses = new ArrayList<>();
        for (ErrorCode errorCode : errorCodes) {
            errorResponses.add(
                    ErrorResponse.builder()
                            .code(errorCode.getErrorCode())
                            .message(messageService.getMessage(errorCode.getErrorCode(), locale))
                            .build()
            );
        }
        return errorResponses;
    }

    private String getHeaderValue(String headerKey) {
        HttpServletRequest request =
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        return request.getHeader(headerKey);
    }
}
