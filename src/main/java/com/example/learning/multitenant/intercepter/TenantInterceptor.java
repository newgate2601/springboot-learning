package com.example.learning.multitenant.intercepter;

import com.example.learning.multitenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

@Component
public class TenantInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) throws Exception {
        String tenantId = request.getHeader("Authorization");
        TenantContext.setTenantContext(tenantId);
        MDC.put("tenantId", tenantId);
        return true;
    }

    // sau khi xử lý request nhưng trước khi trả về client
    @Override
    public void postHandle(@NonNull HttpServletRequest request,
                           @NonNull HttpServletResponse response,
                           @NonNull Object handler,
                           ModelAndView modelAndView) throws Exception {
        clear();
    }

    // sau khi client nhận được response
    @Override
    public void afterCompletion(@NonNull HttpServletRequest request,
                                @NonNull HttpServletResponse response,
                                @NonNull Object handler,
                                Exception ex) throws Exception {
        clear();
    }

    private void clear() {
        TenantContext.clear();
        MDC.clear();
    }
}
