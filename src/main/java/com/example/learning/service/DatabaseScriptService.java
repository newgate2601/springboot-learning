package com.example.learning.service;

import com.example.learning.dto.DatabaseScriptRequest;
import com.zaxxer.hikari.HikariDataSource;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Objects;

@Service
@AllArgsConstructor
@Slf4j
public class DatabaseScriptService {

    @Transactional
    public void executive(DatabaseScriptRequest databaseScriptRequest) {
        HikariDataSource dataSource = newDataSource(databaseScriptRequest);
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(databaseScriptRequest.getScript());
            log.error("Database script executed !!!");
        } catch (Exception e) {
            log.error("Database script wrong !!!");
        } finally {
            dataSource.close();
            log.error("HikariDataSource closed !!!");
        }
    }

    private HikariDataSource newDataSource(DatabaseScriptRequest databaseScriptRequest) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setJdbcUrl("jdbc:postgresql://localhost:5432/" + databaseScriptRequest.getDatabaseName());
        dataSource.setUsername("postgres");
        dataSource.setPassword("101119");
        dataSource.setMaximumPoolSize(1);
        if (Objects.nonNull(databaseScriptRequest.getSchemaName())) {
            dataSource.setSchema(databaseScriptRequest.getSchemaName());
        }
        return dataSource;
    }
}
