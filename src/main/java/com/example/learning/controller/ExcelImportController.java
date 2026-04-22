package com.example.learning.controller;

import com.example.learning.dto.ExcelImportResult;
import com.example.learning.service.excel.importing.ExcelImportService;
import io.swagger.v3.oas.annotations.Operation;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/api/v1/excel-import")
@RequiredArgsConstructor
@CrossOrigin
public class ExcelImportController {

    private static final String EXCEL_MEDIA_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final ExcelImportService excelImportService;

    @GetMapping("/template")
    @Operation(summary = "Tai template import Excel")
    public ResponseEntity<StreamingResponseBody> downloadTemplate() {
        StreamingResponseBody responseBody = outputStream -> excelImportService.writeTemplate(outputStream);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(EXCEL_MEDIA_TYPE))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("excel-import-template.xlsx").build().toString()
                )
                .body(responseBody);
    }

    @GetMapping("/sample-files")
    @Operation(summary = "Lay danh sach moc row sample ho tro")
    public ResponseEntity<List<Integer>> getSupportedSampleRowCounts() {
        return ResponseEntity.ok(excelImportService.getSupportedSampleRowCounts());
    }

    @GetMapping("/sample-files/{rowCount}")
    @Operation(summary = "Tai file sample import Excel theo so row")
    public ResponseEntity<StreamingResponseBody> downloadSampleByRowCount(@PathVariable int rowCount) {
        StreamingResponseBody responseBody = outputStream -> excelImportService.writeSampleData(outputStream, rowCount);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(EXCEL_MEDIA_TYPE))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename("excel-import-sample-" + rowCount + ".xlsx")
                                .build()
                                .toString()
                )
                .body(responseBody);
    }

    @PostMapping(value = "/poi", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Import Excel bang Apache POI")
    public ResponseEntity<ExcelImportResult> importByPoi(
            @RequestParam("file") MultipartFile file,
            @RequestParam(name = "resetTable", defaultValue = "true") boolean resetTable
    ) throws IOException {
        return ResponseEntity.ok(excelImportService.importByPoi(file, resetTable));
    }

    @PostMapping(value = "/poi/chunk-5000", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Import Excel bang Apache POI theo chunk 5000 dong")
    public ResponseEntity<ExcelImportResult> importByPoiChunk5000(
            @RequestParam("file") MultipartFile file,
            @RequestParam(name = "resetTable", defaultValue = "true") boolean resetTable
    ) throws IOException {
        return ResponseEntity.ok(excelImportService.importByPoiInChunks(file, resetTable));
    }

    @PostMapping(value = "/poi-sax", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Import Excel bang Apache POI SAX")
    public ResponseEntity<ExcelImportResult> importByPoiSax(
            @RequestParam("file") MultipartFile file,
            @RequestParam(name = "resetTable", defaultValue = "true") boolean resetTable
    ) throws IOException {
        return ResponseEntity.ok(excelImportService.importByPoiSax(file, resetTable));
    }

    @PostMapping(value = "/poi-sax/chunk-5000", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Import Excel bang Apache POI SAX theo chunk 5000 dong")
    public ResponseEntity<ExcelImportResult> importByPoiSaxChunk5000(
            @RequestParam("file") MultipartFile file,
            @RequestParam(name = "resetTable", defaultValue = "true") boolean resetTable
    ) throws IOException {
        return ResponseEntity.ok(excelImportService.importByPoiSaxInChunks(file, resetTable));
    }

    @PostMapping(value = "/fastexcel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Import Excel bang FastExcel")
    public ResponseEntity<ExcelImportResult> importByFastExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam(name = "resetTable", defaultValue = "true") boolean resetTable
    ) throws IOException {
        return ResponseEntity.ok(excelImportService.importByFastExcel(file, resetTable));
    }

    @PostMapping(value = "/fastexcel/chunk-5000", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Import Excel bang FastExcel theo chunk 5000 dong")
    public ResponseEntity<ExcelImportResult> importByFastExcelChunk5000(
            @RequestParam("file") MultipartFile file,
            @RequestParam(name = "resetTable", defaultValue = "true") boolean resetTable
    ) throws IOException {
        return ResponseEntity.ok(excelImportService.importByFastExcelInChunks(file, resetTable));
    }
}
