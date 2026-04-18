package com.example.learning.controller;

import com.example.learning.service.OrderFastExcelJdbcBenchmarkService;
import io.swagger.v3.oas.annotations.Operation;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/api/v1/benchmark/orders/fastexcel-jdbc")
@RequiredArgsConstructor
@CrossOrigin
public class OrderFastExcelJdbcBenchmarkController {

    private static final String EXCEL_MEDIA_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final OrderFastExcelJdbcBenchmarkService orderFastExcelJdbcBenchmarkService;

    @GetMapping("/export-cursor")
    @Operation(summary = "Benchmark export orders bang JDBC cursor + FastExcel")
    public ResponseEntity<StreamingResponseBody> exportOrdersByCursor() {
        StreamingResponseBody responseBody = outputStream -> writeSafely(outputStream, ExportMode.CURSOR);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(EXCEL_MEDIA_TYPE))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("orders-fastexcel-jdbc-cursor.xlsx").build().toString()
                )
                .body(responseBody);
    }

    @GetMapping("/export-keyset-batch")
    @Operation(summary = "Benchmark export orders bang JDBC keyset batch ResultSet + FastExcel")
    public ResponseEntity<StreamingResponseBody> exportOrdersByKeysetBatch() {
        StreamingResponseBody responseBody = outputStream -> writeSafely(outputStream, ExportMode.KEYSET_BATCH);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(EXCEL_MEDIA_TYPE))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("orders-fastexcel-jdbc-keyset-batch.xlsx").build().toString()
                )
                .body(responseBody);
    }

    private void writeSafely(java.io.OutputStream outputStream, ExportMode exportMode) throws IOException {
        switch (exportMode) {
            case CURSOR -> orderFastExcelJdbcBenchmarkService.exportByCursor(outputStream);
            case KEYSET_BATCH -> orderFastExcelJdbcBenchmarkService.exportByKeysetBatch(outputStream);
        }
    }

    private enum ExportMode {
        CURSOR,
        KEYSET_BATCH
    }
}
