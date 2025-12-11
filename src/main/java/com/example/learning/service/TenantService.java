package com.example.learning.service;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;


@AllArgsConstructor
@Service
public class TenantService {
    private final DataSource dataSource;

    @SneakyThrows
    public void createTenant(String tenantName) {
        Connection conn = dataSource.getConnection();
        Statement stmt = conn.createStatement();
        stmt.executeUpdate("CREATE DATABASE product_" + tenantName);
        stmt.executeUpdate("CREATE DATABASE sale_" + tenantName);
        createProductDatabase(tenantName);
        createSaleDatabase(tenantName);
    }

    private void createProductDatabase(String tenantName) throws Exception {
        try (Connection productConn = getNewConnection("product_" + tenantName);
             Statement stmt = productConn.createStatement()) {

            String sql = """
                CREATE TABLE IF NOT EXISTS products (
                    id SERIAL PRIMARY KEY,
                    name VARCHAR(255),
                    category_id INT
                );
                
                CREATE TABLE IF NOT EXISTS categories (
                    id SERIAL PRIMARY KEY,
                    name VARCHAR(255),
                    description TEXT
                );
                """;

            stmt.executeUpdate(sql);
        }
    }

    private void createSaleDatabase(String tenantName) throws Exception {
        try (Connection saleConn = getNewConnection("sale_" + tenantName);
             Statement stmt = saleConn.createStatement()) {

            String sql = """
                CREATE TABLE IF NOT EXISTS orders (
                    id SERIAL PRIMARY KEY,
                    name VARCHAR(255),
                    order_date TIMESTAMP DEFAULT NOW()
                );
                
                CREATE TABLE IF NOT EXISTS order_lines (
                    id SERIAL PRIMARY KEY,
                    name VARCHAR(255),
                    order_id INT,
                    product_id INT,
                    quantity INT,
                    price DECIMAL(10,2)
                );
                """;

            stmt.executeUpdate(sql);
        }
    }

    private Connection getNewConnection(String databaseName) throws Exception {
        var url = dataSource.getConnection().getMetaData().getURL();
        var baseUrl = url.substring(0, url.lastIndexOf("/"));
        var newUrl = baseUrl + "/" + databaseName;

        return java.sql.DriverManager.getConnection(
                newUrl,
                "postgres",
                "101119"
        );
    }

}
