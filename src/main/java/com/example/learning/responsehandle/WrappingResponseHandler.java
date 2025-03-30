package com.example.learning.responsehandle;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@ControllerAdvice
@Slf4j
public class WrappingResponseHandler implements ResponseBodyAdvice<Object> {
    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
        // apply with API not return IdNameResponse
//        return !returnType.getParameterType().equals(IdNameResponse.class);
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  org.springframework.http.server.ServerHttpRequest request,
                                  org.springframework.http.server.ServerHttpResponse response) {
        log.error("handle response");
        // if is exception -> return exception
        if (body instanceof CustomResponse
//                && Objects.nonNull(((CustomResponse<?>) body).getStatus())
        ) {
            return body;
        }
        // if success -> format response
        return CustomResponse.ok(body);
    }
}
