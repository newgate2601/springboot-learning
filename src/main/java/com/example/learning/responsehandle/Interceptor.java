//package com.example.learning.responsehandle;
//
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import org.springframework.stereotype.Component;
//import org.springframework.web.servlet.HandlerInterceptor;
//import org.springframework.web.servlet.ModelAndView;
//
//@Component
//public class Interceptor implements HandlerInterceptor {
//    @Override
//    public void postHandle(HttpServletRequest request,
//                           HttpServletResponse response,
//                           Object handler,
//                           ModelAndView modelAndView) throws Exception {
//        System.out.println("Controller xử lý xong! Trạng thái response: " + response.getStatus());
//    }
//}
