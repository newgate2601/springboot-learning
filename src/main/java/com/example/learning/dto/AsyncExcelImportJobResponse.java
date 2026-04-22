package com.example.learning.dto;

import com.example.learning.entity.AsyncExcelImportJobStatus;
import java.time.Instant;
import java.util.UUID;

public record AsyncExcelImportJobResponse(
        UUID id,
        AsyncExcelImportJobStatus status,
        String reader,
        String objectKey,
        String fileName,
        String contentType,
        Long fileSize,
        boolean resetTable,
        int processedRows,
        int importedRows,
        int failedRows,
        String message,
        Instant createdAt,
        Instant expiresAt,
        Instant startedAt,
        Instant finishedAt,
        Instant updatedAt
) {
}
