package com.example.learning.multitenant;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
// su dung de chua thong tin tenant cua request context hien tai
public class TenantContext {
    private static ThreadLocal<String> tenantContext = new ThreadLocal<>();

    public static void setTenantContext(String tenantId) {
        tenantContext.set(tenantId);
    }

    public static String getCurrentTenantId() {
        return tenantContext.get();
    }

    public static void clear() {
        tenantContext.remove();
    }
}
