package com.example.learning.controller;

import com.example.learning.service.OrderCsvJdbcBenchmarkService;
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
@RequestMapping("/api/v1/benchmark/orders/csv-jdbc")
@RequiredArgsConstructor
@CrossOrigin
public class OrderCsvJdbcBenchmarkController {

    private static final String CSV_MEDIA_TYPE = "text/csv";
    private static final String ZIP_MEDIA_TYPE = "application/zip";

    private final OrderCsvJdbcBenchmarkService orderCsvJdbcBenchmarkService;

    @GetMapping("/export-cursor")
    @Operation(summary = "Benchmark export orders bang JDBC cursor + CSV")
    public ResponseEntity<StreamingResponseBody> exportCsvByCursor() {
        return buildResponse(
                ExportFormat.CSV,
                "orders-csv-jdbc-cursor.csv",
                outputStream -> orderCsvJdbcBenchmarkService.exportCsvByCursor(outputStream)
        );
    }

    @GetMapping("/export-keyset-batch")
    @Operation(summary = "Benchmark export orders bang JDBC keyset batch ResultSet + CSV")
    public ResponseEntity<StreamingResponseBody> exportCsvByKeysetBatch() {
        return buildResponse(
                ExportFormat.CSV,
                "orders-csv-jdbc-keyset-batch.csv",
                outputStream -> orderCsvJdbcBenchmarkService.exportCsvByKeysetBatch(outputStream)
        );
    }

    @GetMapping("/export-cursor-zip")
    @Operation(summary = "Benchmark export orders bang JDBC cursor + ZIP CSV")
    public ResponseEntity<StreamingResponseBody> exportZipCsvByCursor() {
        return buildResponse(
                ExportFormat.ZIP,
                "orders-csv-jdbc-cursor.zip",
                outputStream -> orderCsvJdbcBenchmarkService.exportZipCsvByCursor(outputStream)
        );
    }

    @GetMapping("/export-keyset-batch-zip")
    @Operation(summary = "Benchmark export orders bang JDBC keyset batch ResultSet + ZIP CSV")
    public ResponseEntity<StreamingResponseBody> exportZipCsvByKeysetBatch() {
        return buildResponse(
                ExportFormat.ZIP,
                "orders-csv-jdbc-keyset-batch.zip",
                outputStream -> orderCsvJdbcBenchmarkService.exportZipCsvByKeysetBatch(outputStream)
        );
    }

    private ResponseEntity<StreamingResponseBody> buildResponse(
            ExportFormat exportFormat,
            String filename,
            CsvStreamingWriter writer
    ) {
        StreamingResponseBody responseBody = outputStream -> writer.write(outputStream);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(exportFormat.mediaType))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString()
                )
                .body(responseBody);
    }

    @FunctionalInterface
    private interface CsvStreamingWriter {
        void write(java.io.OutputStream outputStream) throws IOException;
    }

    private enum ExportFormat {
        CSV(CSV_MEDIA_TYPE),
        ZIP(ZIP_MEDIA_TYPE);

        private final String mediaType;

        ExportFormat(String mediaType) {
            this.mediaType = mediaType;
        }
    }
}
