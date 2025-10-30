package com.example.learning.controller;

import lombok.Data;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;

@RestController
@RequestMapping("/api/v1/dynamic-report")
@Slf4j
public class DynamicTableController {
    @GetMapping(value = "", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> generateDynamicTablesReport() {
        try {
            // 1. Prepare dynamic data using Map instead of Object
            List<Map<String, Object>> reportData = prepareDynamicData();

            // 2. Load and compile template
            File file = ResourceUtils.getFile("classpath:dynamic_tables_report.jrxml");
            InputStream input = new FileInputStream(file);
            JasperReport jasperReport = JasperCompileManager.compileReport(input);

            // 3. Create datasource - Use JRMapCollectionDataSource
            JRDataSource source = new JRMapCollectionDataSource(new ArrayList<>(reportData));

            // 4. Parameters
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("createdBy", "Dynamic Report System");

            // 5. Fill and export report
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, source);
            byte[] pdfBytes = JasperExportManager.exportReportToPdf(jasperPrint);

            // 6. Set response headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition.builder("attachment")
                    .filename("dynamic_tables_report.pdf")
                    .build());
            headers.setContentLength(pdfBytes.length);

            log.info("Dynamic tables PDF generated successfully!");
            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Error generating dynamic tables report: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Error generating report: " + e.getMessage()).getBytes());
        }
    }

    private List<Map<String, Object>> prepareDynamicData() {
        List<Map<String, Object>> data = new ArrayList<>();

        // Employee 1
        Map<String, Object> emp1 = new HashMap<>();
        emp1.put("employeeId", 1);
        emp1.put("employeeName", "Nguyen Van A");
        emp1.put("employeeDepartment", "IT Department");
        emp1.put("employeeSalary", 5500);

        // TABLE 1: Salary History
        List<Map<String, Object>> salaries1 = Arrays.asList(
                createSalaryDetail("Q1 2024", 5200.0, 500.0),
                createSalaryDetail("Q2 2024", 5300.0, 600.0),
                createSalaryDetail("Q3 2024", 5400.0, 700.0),
                createSalaryDetail("Q4 2024", 5500.0, 800.0)
        );

        // TABLE 2: Projects
        List<Map<String, Object>> projects1 = Arrays.asList(
                createProjectDetail("E-Commerce Platform", "Lead Developer", 40, "In Progress"),
                createProjectDetail("Mobile Application", "Technical Consultant", 20, "Completed"),
                createProjectDetail("Internal Tools", "Code Reviewer", 10, "In Progress")
        );

        // TABLE 3: Skills
        List<Map<String, Object>> skills1 = Arrays.asList(
                createSkillDetail("Java Spring Boot", "Expert", "5 years"),
                createSkillDetail("PostgreSQL", "Advanced", "3 years"),
                createSkillDetail("Docker & Kubernetes", "Intermediate", "2 years"),
                createSkillDetail("AWS Cloud", "Intermediate", "2 years"),
                createSkillDetail("React JS", "Beginner", "1 year")
        );

        emp1.put("salaryDetails", salaries1);
        emp1.put("projectDetails", projects1);
        emp1.put("skillDetails", skills1);
        data.add(emp1);

        // Employee 2
        Map<String, Object> emp2 = new HashMap<>();
        emp2.put("employeeId", 2);
        emp2.put("employeeName", "Tran Thi B");
        emp2.put("employeeDepartment", "Marketing");
        emp2.put("employeeSalary", 4200);

        List<Map<String, Object>> salaries2 = Arrays.asList(
                createSalaryDetail("Q1 2024", 4000.0, 200.0),
                createSalaryDetail("Q2 2024", 4200.0, 250.0)
        );

        List<Map<String, Object>> projects2 = Arrays.asList(
                createProjectDetail("Brand Campaign 2024", "Project Manager", 35, "Completed"),
                createProjectDetail("Social Media Strategy", "Team Lead", 25, "In Progress")
        );

        List<Map<String, Object>> skills2 = Arrays.asList(
                createSkillDetail("Digital Marketing", "Expert", "6 years"),
                createSkillDetail("SEO/SEM", "Advanced", "4 years"),
                createSkillDetail("Content Strategy", "Intermediate", "3 years")
        );

        emp2.put("salaryDetails", salaries2);
        emp2.put("projectDetails", projects2);
        emp2.put("skillDetails", skills2);
        data.add(emp2);

        return data;
    }

    private Map<String, Object> createSalaryDetail(String period, Double amount, Double bonus) {
        Map<String, Object> detail = new HashMap<>();
        detail.put("period", period);
        detail.put("amount", amount);
        detail.put("bonus", bonus);
        return detail;
    }

    private Map<String, Object> createProjectDetail(String projectName, String role, Integer hours, String status) {
        Map<String, Object> detail = new HashMap<>();
        detail.put("projectName", projectName);
        detail.put("role", role);
        detail.put("hours", hours);
        detail.put("status", status);
        return detail;
    }

    private Map<String, Object> createSkillDetail(String skillName, String level, String experience) {
        Map<String, Object> detail = new HashMap<>();
        detail.put("skillName", skillName);
        detail.put("level", level);
        detail.put("experience", experience);
        return detail;
    }
}
