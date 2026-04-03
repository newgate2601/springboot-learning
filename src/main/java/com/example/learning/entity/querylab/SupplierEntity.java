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
        name = "ql_supplier",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_ql_supplier_code", columnNames = "supplier_code")
        },
        indexes = {
                @Index(name = "idx_ql_supplier_country", columnList = "country_code"),
                @Index(name = "idx_ql_supplier_active", columnList = "active")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // PK của nhà cung cấp.

    @Column(name = "supplier_code", nullable = false, length = 40)
    private String supplierCode; // Mã NCC dùng xuyên suốt trên PO và hợp đồng.

    @Column(name = "supplier_name", nullable = false, length = 200)
    private String supplierName; // Tên đầy đủ của nhà cung cấp.

    @Column(name = "tax_code", nullable = false, length = 30)
    private String taxCode; // Mã số thuế để đối soát hóa đơn.

    @Column(name = "country_code", nullable = false, length = 5)
    private String countryCode; // Quốc gia của nhà cung cấp.

    @Builder.Default
    @Column(name = "rating_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal ratingScore = BigDecimal.ZERO; // Điểm đánh giá chất lượng NCC.

    @Builder.Default
    @Column(name = "active", nullable = false)
    private Boolean active = true; // Cho phép dùng NCC trong giao dịch mới.
}

