package com.example.learning.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record OrderExportRow(
        Long id,
        String orderNo,
        Long customerId,
        String status,
        BigDecimal totalAmount,
        OffsetDateTime orderDate,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
