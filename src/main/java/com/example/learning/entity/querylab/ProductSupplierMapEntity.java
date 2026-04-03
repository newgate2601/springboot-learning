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
        name = "ql_product_supplier_map",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_ql_psm_product_supplier", columnNames = {"product_id", "supplier_id"})
        },
        indexes = {
                @Index(name = "idx_ql_psm_product", columnList = "product_id"),
                @Index(name = "idx_ql_psm_supplier", columnList = "supplier_id"),
                @Index(name = "idx_ql_psm_primary", columnList = "primary_source")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSupplierMapEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // PK của bảng mapping product-supplier.

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false, foreignKey = @ForeignKey(name = "fk_ql_psm_product"))
    private ProductEntity product; // Sản phẩm được cung cấp bởi NCC.

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false, foreignKey = @ForeignKey(name = "fk_ql_psm_supplier"))
    private SupplierEntity supplier; // Nhà cung cấp có thể bán sản phẩm này.

    @Column(name = "supplier_sku", nullable = false, length = 80)
    private String supplierSku; // Mã hàng riêng của NCC, thường dùng khi đặt mua.

    @Column(name = "lead_time_days", nullable = false)
    private Integer leadTimeDays; // Số ngày đặt đến lúc nhận hàng.

    @Builder.Default
    @Column(name = "min_order_qty", nullable = false, precision = 19, scale = 4)
    private BigDecimal minOrderQty = BigDecimal.ZERO; // Số lượng tối thiểu mỗi lần mua.

    @Builder.Default
    @Column(name = "contract_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal contractPrice = BigDecimal.ZERO; // Giá hợp đồng đang hiệu lực.

    @Builder.Default
    @Column(name = "primary_source", nullable = false)
    private Boolean primarySource = false; // Cờ NCC ưu tiên để planning mua hàng.
}

