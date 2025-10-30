package com.example.learning.controller;

import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.JRCsvExporter;
import net.sf.jasperreports.export.SimpleCsvExporterConfiguration;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleWriterExporterOutput;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/v1/report")
@AllArgsConstructor
@Slf4j
public class JasperController {

    @GetMapping(value = "/report-pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> generateReportPdf() {
        List<Employee> employees = new ArrayList<>();
        employees.add(
                Employee.builder()
                        .id(1)
                        .name("n1")
                        .designation("d1")
                        .oraganization("o1")
                        .salary(1)
                        .build()
        );
        employees.add(
                Employee.builder()
                        .id(2)
                        .name("n2")
                        .designation("d2")
                        .oraganization("o2")
                        .salary(2)
                        .build()
        );

        try {
            File file = ResourceUtils.getFile("classpath:bbdchdgtgt.jrxml");
            InputStream input = new FileInputStream(file);

            JasperReport jasperReport = JasperCompileManager.compileReport(input);
            JRBeanCollectionDataSource source = new JRBeanCollectionDataSource(employees);

            Map<String, Object> parameters = new HashMap<>();
            parameters.put("createdBy", "JavaHelper.org"); // Giữ nguyên

            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, source);
            byte[] pdfBytes = JasperExportManager.exportReportToPdf(jasperPrint);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition.builder("attachment")
                    .filename("EmployeeReport.pdf")
                    .build());

            System.out.println("PDF Generated Successfully!");
            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Ex {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Error generating report: " + e.getMessage()).getBytes());
        }
    }
}
