package com.example.learning.dto;

import com.example.learning.entity.AsyncExcelImportJobStatus;
import java.util.UUID;

public record AsyncExcelImportPresignUploadResponse(
        UUID jobId,
        AsyncExcelImportJobStatus status,
        String objectKey,
        String uploadUrl,
        long expiresInMinutes
) {
}
