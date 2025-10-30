package com.example.learning.controller;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.io.*;
import java.util.*;

@RestController
@RequestMapping("/api/adjustment-invoice")
@CrossOrigin(origins = "*")
public class AdjustmentInvoiceAPI {

    @GetMapping("")
    public ResponseEntity<byte[]> generateInvoiceAdjustmentReport() {
        try {
            // Tạo datasource chứa tất cả dữ liệu (dùng cho main dataset)
            List<MainData> mainDataList = createMainData();
            List<ProductInfo> fakeProducts = createFakeProducts();

            byte[] pdfBytes = generatePDF(mainDataList, fakeProducts);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition.builder("attachment")
                    .filename("BienBanDieuChinhHoaDon_HD2024001.pdf")
                    .build());
            headers.setContentLength(pdfBytes.length);

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/test")
    public ResponseEntity<String> testAPI() {
        return ResponseEntity.ok("API is working! Use /api/adjustment-invoice to download PDF");
    }

    private byte[] generatePDF(List<MainData> mainDataList, List<ProductInfo> products) throws Exception {
        // Load JRXML template
        InputStream templateStream = getClass().getResourceAsStream("/bbdchd.jrxml");

        if (templateStream == null) {
            throw new RuntimeException("Template file not found in resources/");
        }

        // Compile JRXML
        JasperReport jasperReport = JasperCompileManager.compileReport(templateStream);

        // Prepare datasource cho main report (fields)
        JRBeanCollectionDataSource mainDataSource = new JRBeanCollectionDataSource(mainDataList);

        // Prepare datasource cho table (subdataset)
        JRBeanCollectionDataSource tableDataSource = new JRBeanCollectionDataSource(products);

        // Truyền table datasource qua parameters
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("table1Dataset", tableDataSource);

        // Fill report với main datasource (chứa fields)
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, mainDataSource);

        // Export to PDF
        return JasperExportManager.exportReportToPdf(jasperPrint);
    }

    private List<MainData> createMainData() {
        List<MainData> mainDataList = new ArrayList<>();
        MainData mainData = new MainData();

        // Set tất cả các fields theo đúng tên trong JRXML
        mainData.setDateNow("15");
        mainData.setMonthNow("12");
        mainData.setYearNow("2024");
        mainData.setRepresentativeA("NGUYỄN VĂN AN");
        mainData.setPositionA("GIÁM ĐỐC");
        mainData.setB("CÔNG TY TNHH CÔNG NGHỆ VIỆT");
        mainData.setAddressB("Tầng 5, Tòa nhà Sunrise, 123 Nguyễn Văn Linh, Quận 7, TP.HCM");
        mainData.setTaxNumberB("0315842679");
        mainData.setRepresentativeB("TRẦN THỊ BÌNH");
        mainData.setPositionB("GIÁM ĐỐC TÀI CHÍNH");
        mainData.setInvoiceNo("HD/2024/001");
        mainData.setSymbol("AB/24E");
        mainData.setCreateTime("10/12/2024");
        mainData.setUpdateReason("Điều chỉnh thông tin sản phẩm do nhập sai mã hàng và tên sản phẩm trong hóa đơn ban đầu");

        mainDataList.add(mainData);
        return mainDataList;
    }

    private List<ProductInfo> createFakeProducts() {
        List<ProductInfo> products = new ArrayList<>();

        String[][] fakeProductsData = {
                {"Laptop Dell XPS 13 9320", "DELL-XPS13-9320"},
                {"Chuột không dây Logitech MX Master 3", "LOG-MX-MASTER-3"}
//                ,
//                {"Bàn phím cơ Keychron K8", "KECHRON-K8-PRO"},
//                {"Màn hình Dell UltraSharp 27\"", "DELL-U2723QE"},
//                {"Dock Station USB-C Dell WD19", "DELL-WD19-TBS"},
//                {"Tai nghe Sony WH-1000XM4", "SONY-WH1000XM4"},
//                {"Webcam Logitech C922 Pro", "LOG-C922-PRO"},
//                {"Ổ cứng SSD Samsung 1TB", "SSD-SAMSUNG-1TB"},
//                {"Router WiFi Asus RT-AX86U", "ASUS-RT-AX86U"},
//                {"Bộ nhớ RAM DDR4 16GB", "RAM-KINGSTON-16GB"}
        };

        for (String[] productData : fakeProductsData) {
            ProductInfo product = new ProductInfo();
            product.setProductNameTable1(productData[0]);
            product.setProductCodeTable1(productData[1]);
            products.add(product);
        }

        return products;
    }

    // Inner class cho Main Data (fields)
    public static class MainData {
        private String dateNow;
        private String monthNow;
        private String yearNow;
        private String representativeA;
        private String positionA;
        private String b;
        private String addressB;
        private String taxNumberB;
        private String representativeB;
        private String positionB;
        private String invoiceNo;
        private String symbol;
        private String createTime;
        private String updateReason;

        // Getter và Setter cho tất cả fields
        public String getDateNow() { return dateNow; }
        public void setDateNow(String dateNow) { this.dateNow = dateNow; }

        public String getMonthNow() { return monthNow; }
        public void setMonthNow(String monthNow) { this.monthNow = monthNow; }

        public String getYearNow() { return yearNow; }
        public void setYearNow(String yearNow) { this.yearNow = yearNow; }

        public String getRepresentativeA() { return representativeA; }
        public void setRepresentativeA(String representativeA) { this.representativeA = representativeA; }

        public String getPositionA() { return positionA; }
        public void setPositionA(String positionA) { this.positionA = positionA; }

        public String getB() { return b; }
        public void setB(String b) { this.b = b; }

        public String getAddressB() { return addressB; }
        public void setAddressB(String addressB) { this.addressB = addressB; }

        public String getTaxNumberB() { return taxNumberB; }
        public void setTaxNumberB(String taxNumberB) { this.taxNumberB = taxNumberB; }

        public String getRepresentativeB() { return representativeB; }
        public void setRepresentativeB(String representativeB) { this.representativeB = representativeB; }

        public String getPositionB() { return positionB; }
        public void setPositionB(String positionB) { this.positionB = positionB; }

        public String getInvoiceNo() { return invoiceNo; }
        public void setInvoiceNo(String invoiceNo) { this.invoiceNo = invoiceNo; }

        public String getSymbol() { return symbol; }
        public void setSymbol(String symbol) { this.symbol = symbol; }

        public String getCreateTime() { return createTime; }
        public void setCreateTime(String createTime) { this.createTime = createTime; }

        public String getUpdateReason() { return updateReason; }
        public void setUpdateReason(String updateReason) { this.updateReason = updateReason; }
    }

    // Inner class cho Product (table data)
    public static class ProductInfo {
        private String productNameTable1;
        private String productCodeTable1;

        public String getProductNameTable1() { return productNameTable1; }
        public void setProductNameTable1(String productNameTable1) { this.productNameTable1 = productNameTable1; }

        public String getProductCodeTable1() { return productCodeTable1; }
        public void setProductCodeTable1(String productCodeTable1) { this.productCodeTable1 = productCodeTable1; }
    }
}