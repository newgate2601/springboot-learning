package com.example.learning.controller;

import com.example.learning.service.OrderStreamingBenchmarkService;
import io.swagger.v3.oas.annotations.Operation;
import java.io.IOException;
import lombok.AllArgsConstructor;
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
@RequestMapping("/api/v1/benchmark/orders/streaming")
@AllArgsConstructor
@CrossOrigin
public class OrderStreamingBenchmarkController {

    private static final String EXCEL_MEDIA_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final OrderStreamingBenchmarkService orderStreamingBenchmarkService;

    @GetMapping("/export-poi")
    @Operation(summary = "Export orders streaming bang Apache POI SXSSF")
    public ResponseEntity<StreamingResponseBody> exportOrdersByPoi() {
        StreamingResponseBody responseBody = outputStream -> writeSafely(outputStream, ExportType.POI);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(EXCEL_MEDIA_TYPE))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("orders-poi-streaming.xlsx").build().toString())
                .body(responseBody);
    }

    @GetMapping("/export-fastexcel")
    @Operation(summary = "Export orders streaming bang FastExcel")
    public ResponseEntity<StreamingResponseBody> exportOrdersByFastExcel() {
        StreamingResponseBody responseBody = outputStream -> writeSafely(outputStream, ExportType.FAST_EXCEL);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(EXCEL_MEDIA_TYPE))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("orders-fastexcel-streaming.xlsx").build().toString())
                .body(responseBody);
    }

    private void writeSafely(java.io.OutputStream outputStream, ExportType exportType) throws IOException {
        switch (exportType) {
            case POI -> orderStreamingBenchmarkService.streamOrdersByPoi(outputStream);
            case FAST_EXCEL -> orderStreamingBenchmarkService.streamOrdersByFastExcel(outputStream);
        }
    }

    private enum ExportType {
        POI,
        FAST_EXCEL
    }
}
