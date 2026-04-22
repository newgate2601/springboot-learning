package com.example.learning.controller;

import com.example.learning.dto.AsyncExcelImportJobResponse;
import com.example.learning.dto.AsyncExcelImportPresignUploadRequest;
import com.example.learning.dto.AsyncExcelImportPresignUploadResponse;
import com.example.learning.service.excel.importing.AsyncExcelImportService;
import io.swagger.v3.oas.annotations.Operation;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin
@RequiredArgsConstructor
@RequestMapping("/api/v1/excel-import/async")
@ConditionalOnBean(AsyncExcelImportService.class)
public class AsyncExcelImportController {

    private final AsyncExcelImportService asyncExcelImportService;

    @PostMapping("/presign-upload")
    @Operation(summary = "Tao job import va presigned PUT URL de upload file Excel len S3")
    public ResponseEntity<AsyncExcelImportPresignUploadResponse> createPresignedUpload(
            @RequestBody AsyncExcelImportPresignUploadRequest request
    ) {
        return ResponseEntity.ok(asyncExcelImportService.createPresignedUpload(request));
    }

    @PostMapping("/jobs/{jobId}/start")
    @Operation(summary = "Xac nhan file da upload len S3 va bat dau import async")
    public ResponseEntity<AsyncExcelImportJobResponse> startJob(@PathVariable UUID jobId) {
        return ResponseEntity.ok(asyncExcelImportService.startJob(jobId));
    }

    @GetMapping("/jobs/{jobId}")
    @Operation(summary = "Lay trang thai import job async")
    public ResponseEntity<AsyncExcelImportJobResponse> getJob(@PathVariable UUID jobId) {
        return ResponseEntity.ok(asyncExcelImportService.getJob(jobId));
    }

    @GetMapping("/jobs/{jobId}/source-download-url")
    @Operation(summary = "Lay presigned GET URL de tai file source tren S3")
    public ResponseEntity<Map<String, String>> getSourceDownloadUrl(@PathVariable UUID jobId) {
        return ResponseEntity.ok(Map.of(
                "downloadUrl",
                asyncExcelImportService.generatePresignedSourceDownloadUrl(jobId)
        ));
    }
}
