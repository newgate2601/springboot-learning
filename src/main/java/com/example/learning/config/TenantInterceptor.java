package com.example.learning.config;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import javax.sql.DataSource;

@Component
public class TenantInterceptor implements HandlerInterceptor {

    private final DataSourceConfig dataSourceConfig;

    public TenantInterceptor(DataSourceConfig dataSourceConfig) {
        this.dataSourceConfig = dataSourceConfig;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String tenantName = request.getParameter("tenantName");

        if (tenantName == null || tenantName.isEmpty()) {
            // ... (Xử lý lỗi thiếu tenantName như cũ)
            return false;
        }

        // Tên database thực tế: Ví dụ product_tenantA
        String actualDbName = "product_" + tenantName;

        // 1. Kiểm tra xem DataSource đã tồn tại chưa
        if (!dataSourceConfig.getTargetDataSources().containsKey(actualDbName)) {
            System.out.println("Tenant " + actualDbName + " chưa tồn tại. Bắt đầu tạo mới.");

            // 2. Tạo Database và chạy Script
            try {

                // 3. Tạo DataSource mới trỏ tới DB vừa tạo
                DataSource newDataSource = DataSourceBuilder.create()
                        .driverClassName("org.postgresql.Driver")
                        .url("jdbc:postgresql://localhost:5432/" + actualDbName)
                        .username("postgres")
                        .password("101119")
                        .build();

                // 4. Đăng ký DataSource mới vào Routing Map
                dataSourceConfig.addTenantDataSource(actualDbName, newDataSource);

            } catch (Exception e) {
                // Xử lý lỗi trong quá trình tạo DB/DataSource
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("Lỗi khi tạo database mới cho tenant " + actualDbName + ": " + e.getMessage());
                TenantContext.clear();
                return false;
            }
        }

        // 5. Thiết lập Tenant Context để Routing DataSource sử dụng
        TenantContext.setCurrentTenant(actualDbName);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        TenantContext.clear();
    }
}