package com.example.learning.multitenant;

import com.example.learning.multitenant.datasourcesetting.DataSourceSettings;
import com.example.learning.multitenant.datasourcesetting.PostgresqlSettings;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
// su dung de quan ly DataSource cua toan bo tenant
public class TenantDataSourceManager {
    private final Map<String, DataSource> dataSources;
    private final String defaultDatabaseName;
    private final DataSource defaultDataSource;
    private final String defaultTenantId;

    public TenantDataSourceManager(DataSource defaultDataSource) {
        this.dataSources = new HashMap<>();
        this.defaultDataSource = defaultDataSource;
        this.defaultDatabaseName = "learning";
        this.defaultTenantId = "";
        dataSources.put(this.defaultTenantId, defaultDataSource);
    }

    public DataSource getDefaultDataSource() {
        log.error("Get default DataSource !!!");
        return defaultDataSource;
    }

    public DataSource getDataSource(@NonNull String tenantId) {
        log.error("Get DataSource with tenantId: " + tenantId);
        DataSource dataSource = dataSources.get(tenantId);
        return dataSource != null ? dataSource : createDataSource(tenantId);
    }

    public void removeDataSource(String tenantId) {
        DataSource dataSource = dataSources.remove(tenantId);
        ((HikariDataSource) dataSource).close();
    }

    private DataSource createDataSource(String tenantId) {
        PostgresqlSettings settings = (PostgresqlSettings) getDataSourceSettings() ; // thong thuong se lay config ben uaa

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setDriverClassName(settings.getDriverClassName());
        hikariConfig.setJdbcUrl(createJdbcUrl(tenantId, settings));
        hikariConfig.setUsername(settings.getUsername());
        hikariConfig.setPassword(settings.getPassword());
        hikariConfig.setMinimumIdle(settings.getMinimumIdle());
        hikariConfig.setMaximumPoolSize(settings.getMaximumPoolSize());
        hikariConfig.setIdleTimeout(settings.getIdleTimeout());
        hikariConfig.setMaxLifetime(settings.getMaxLifetime());
        hikariConfig.setConnectionTimeout(settings.getConnectionTimeout());
        DataSource dataSource = new HikariDataSource(hikariConfig);
        dataSources.put(tenantId, dataSource);
        log.error("Create DataSource successful with tenantId: " + tenantId);
        return dataSource;
    }

    private PostgresqlSettings getDataSourceSettings() {
        PostgresqlSettings settings = new PostgresqlSettings();
        settings.setServerHost("localhost");
        settings.setServerPort(5432);
        settings.setDatabaseName(defaultDatabaseName);
//        settings.setRequestParameters(String.valueOf(TenantContext.getCurrentTenantId()));
        settings.setUsername("postgres");
        settings.setPassword("101119");

        settings.setMinimumIdle(5);
        settings.setMaximumPoolSize(20);
        settings.setIdleTimeout(5000L);
        settings.setMaxLifetime(300000L);
        settings.setConnectionTimeout(30000L);

        return settings;
    }

    private String createJdbcUrl(@NonNull String tenantId, @NonNull DataSourceSettings settings) {
//        String url = settings.getBaseUrl() + defaultDatabaseName + tenantId + settings.getRequestParameters();
        String url = settings.getBaseUrl() + defaultDatabaseName + tenantId + "?currentSchema=tenant";
        log.error("Create DataSource with url: " + url);
        return url;
    }
}
