package com.example.learning.controller;

import com.example.learning.service.excel.export.OrderNonStreamingBenchmarkService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.AllArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/benchmark/orders/non-streaming")
@AllArgsConstructor
@CrossOrigin
public class OrderNonStreamingBenchmarkController {

    private static final String EXCEL_MEDIA_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final OrderNonStreamingBenchmarkService orderNonStreamingBenchmarkService;

    @GetMapping("/export-poi")
    @Operation(summary = "Export orders non-streaming bang Apache POI")
    public ResponseEntity<byte[]> exportOrdersByPoi() throws Exception {
        byte[] fileContent = orderNonStreamingBenchmarkService.exportOrdersByPoi();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(EXCEL_MEDIA_TYPE))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("orders-poi-non-streaming.xlsx").build().toString())
                .contentLength(fileContent.length)
                .body(fileContent);
    }

    @GetMapping("/export-fastexcel")
    @Operation(summary = "Export orders non-streaming bang FastExcel")
    public ResponseEntity<byte[]> exportOrdersByFastExcel() throws Exception {
        byte[] fileContent = orderNonStreamingBenchmarkService.exportOrdersByFastExcel();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(EXCEL_MEDIA_TYPE))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("orders-fastexcel-non-streaming.xlsx").build().toString())
                .contentLength(fileContent.length)
                .body(fileContent);
    }
}
