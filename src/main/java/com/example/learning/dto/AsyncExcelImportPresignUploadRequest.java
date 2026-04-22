package com.example.learning.dto;

public record AsyncExcelImportPresignUploadRequest(
        String fileName,
        String contentType,
        String keyPrefix,
        boolean resetTable
) {
}
