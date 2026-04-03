package com.example.learning.entity.querylab;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(
        name = "ql_customer",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_ql_customer_code", columnNames = "customer_code")
        },
        indexes = {
                @Index(name = "idx_ql_customer_segment", columnList = "segment"),
                @Index(name = "idx_ql_customer_active", columnList = "active")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // PK của khách hàng.

    @Column(name = "customer_code", nullable = false, length = 40)
    private String customerCode; // Mã KH để join qua ERP/CRM.

    @Column(name = "customer_name", nullable = false, length = 200)
    private String customerName; // Tên khách hàng hiển thị trên SO.

    @Column(name = "segment", nullable = false, length = 50)
    private String segment; // Nhóm KH: RETAIL, B2B, ENTERPRISE.

    @Column(name = "country_code", nullable = false, length = 5)
    private String countryCode; // Quốc gia giao dịch chính.

    @Builder.Default
    @Column(name = "credit_limit", nullable = false, precision = 18, scale = 2)
    private BigDecimal creditLimit = BigDecimal.ZERO; // Hạn mức công nợ tối đa.

    @Builder.Default
    @Column(name = "active", nullable = false)
    private Boolean active = true; // Cho phép tạo SO mới cho KH này.
}

