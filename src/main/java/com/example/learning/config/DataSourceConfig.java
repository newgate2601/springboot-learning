package com.example.learning.config;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import javax.sql.DataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class DataSourceConfig {
    private static final String BASE_URL = "jdbc:postgresql://localhost:5432/";
    private static final String USERNAME = "postgres";
    private static final String PASSWORD = "101119";

    private TenantRoutingDataSource routingDataSource;
    private Map<Object, Object> targetDataSources;

    @Bean
    @Primary
    public DataSource tenantDataSource() {
        this.routingDataSource = new TenantRoutingDataSource();
        this.targetDataSources = new HashMap<>();

        DataSource defaultDataSource = createDataSource("postgres");
        targetDataSources.put("default", defaultDataSource);

        routingDataSource.setTargetDataSources(targetDataSources);
        routingDataSource.setDefaultTargetDataSource(defaultDataSource);
        routingDataSource.afterPropertiesSet();

        return routingDataSource;
    }

    // Phương thức công khai để thêm DataSource mới vào routing map
    public void addTenantDataSource(String tenantId, DataSource dataSource) {
        // Thêm vào Map
        this.targetDataSources.put(tenantId, dataSource);

        // Cập nhật TargetDataSources trong RoutingDataSource
        this.routingDataSource.setTargetDataSources(this.targetDataSources);

        // Gọi afterPropertiesSet để đảm bảo Spring biết về thay đổi này
        this.routingDataSource.afterPropertiesSet();
        System.out.println(">>> Đã cấu hình DataSource mới cho Tenant: " + tenantId);
    }

    // Hàm tạo DataSource (giữ nguyên)
    private DataSource createDataSource(String dbName) {
        // ... (Code tạo DataSource như cũ)
        return DataSourceBuilder.create()
                .driverClassName("org.postgresql.Driver")
                .url(BASE_URL + dbName)
                .username(USERNAME)
                .password(PASSWORD)
                .build();
    }

    public Map<Object, Object> getTargetDataSources() {
        return targetDataSources;
    }
}
