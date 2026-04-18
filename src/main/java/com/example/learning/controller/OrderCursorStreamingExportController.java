package com.example.learning.controller;

import com.example.learning.service.OrderCursorStreamingExportService;
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
@RequestMapping("/api/v1/benchmark/orders/cursor-streaming")
@RequiredArgsConstructor
@CrossOrigin
public class OrderCursorStreamingExportController {

    private static final String EXCEL_MEDIA_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final OrderCursorStreamingExportService orderCursorStreamingExportService;

    @GetMapping("/export-fastexcel")
    @Operation(summary = "Export orders bang JDBC cursor + FastExcel")
    public ResponseEntity<StreamingResponseBody> exportOrdersByCursor() {
        StreamingResponseBody responseBody = outputStream -> writeSafely(outputStream);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(EXCEL_MEDIA_TYPE))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("orders-fastexcel-cursor-streaming.xlsx").build().toString()
                )
                .body(responseBody);
    }

    private void writeSafely(java.io.OutputStream outputStream) throws IOException {
        orderCursorStreamingExportService.exportOrders(outputStream);
    }
}
