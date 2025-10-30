package com.example.learning.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/table")
@CrossOrigin(origins = "*")
public class TableController {

    public static class NutritionData {
        private String name;
        private String code;

        public NutritionData(String name, String code) {
            this.name = name;
            this.code = code;
        }

        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
    }

    public static class Table2Data {
        private String nameTable2;
        private String codeTable2;

        public Table2Data(String nameTable2, String codeTable2) {
            this.nameTable2 = nameTable2;
            this.codeTable2 = codeTable2;
        }

        // Getters and setters
        public String getNameTable2() { return nameTable2; }
        public void setNameTable2(String nameTable2) { this.nameTable2 = nameTable2; }
        public String getCodeTable2() { return codeTable2; }
        public void setCodeTable2(String codeTable2) { this.codeTable2 = codeTable2; }
    }

    // Generate fake data for table 1
    private List<NutritionData> generateFakeData() {
        List<NutritionData> data = new ArrayList<>();
        data.add(new NutritionData("Vitamin A", "VIT-A-001"));
//        data.add(new NutritionData("Vitamin B12", "VIT-B12-002"));
//        data.add(new NutritionData("Vitamin C", "VIT-C-003"));
//        data.add(new NutritionData("Vitamin D", "VIT-D-004"));
//        data.add(new NutritionData("Calcium", "CAL-005"));
        return data;
    }

    // Generate fake data for table 2
    private List<Table2Data> generateFakeDataTable2() {
        List<Table2Data> data = new ArrayList<>();
        data.add(new Table2Data("Product A", "PROD-A-100"));
        data.add(new Table2Data("Product B", "PROD-B-200"));
//        data.add(new Table2Data("Product C", "PROD-C-300"));
//        data.add(new Table2Data("Product D", "PROD-D-400"));
//        data.add(new Table2Data("Product E", "PROD-E-500"));
        return data;
    }

    @GetMapping("")
    public ResponseEntity<Resource> generatePdfReport() {
        try {
            // Generate fake data for both tables
            List<NutritionData> nutritionData = generateFakeData();
            List<Table2Data> table2Data = generateFakeDataTable2();

            // Load Jasper report template
            InputStream templateStream = getClass().getResourceAsStream("/table.jrxml");

            if (templateStream == null) {
                throw new RuntimeException("Jasper template not found!");
            }

            // Compile Jasper report
            JasperReport jasperReport = JasperCompileManager.compileReport(templateStream);

            // Create data sources for both tables
            JRBeanCollectionDataSource nutritionDataSource = new JRBeanCollectionDataSource(nutritionData);
            JRBeanCollectionDataSource table2DataSource = new JRBeanCollectionDataSource(table2Data);

            // Set parameters for both datasets
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("nutritionDataset", nutritionDataSource);
            parameters.put("datasetTabe2", table2DataSource); // Note: Fix typo in parameter name if needed

            // Fill report
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, new JREmptyDataSource());

            // Export to PDF
            byte[] pdfBytes = JasperExportManager.exportReportToPdf(jasperPrint);

            // Create response
            ByteArrayResource resource = new ByteArrayResource(pdfBytes);

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=nutrition-report.pdf");
            headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
            headers.add("Pragma", "no-cache");
            headers.add("Expires", "0");

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(pdfBytes.length)
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(resource);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}