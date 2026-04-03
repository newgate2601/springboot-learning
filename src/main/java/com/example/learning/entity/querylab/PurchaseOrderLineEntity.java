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
        name = "ql_purchase_order_line",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_ql_po_line_no", columnNames = {"purchase_order_id", "line_no"})
        },
        indexes = {
                @Index(name = "idx_ql_po_line_po", columnList = "purchase_order_id"),
                @Index(name = "idx_ql_po_line_product", columnList = "product_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderLineEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // PK của dòng chi tiết PO.

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_order_id", nullable = false, foreignKey = @ForeignKey(name = "fk_ql_po_line_po"))
    private PurchaseOrderEntity purchaseOrder; // Header PO chứa dòng này.

    @Column(name = "line_no", nullable = false)
    private Integer lineNo; // Số thứ tự dòng trên PO.

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false, foreignKey = @ForeignKey(name = "fk_ql_po_line_product"))
    private ProductEntity product; // Sản phẩm được mua.

    @Builder.Default
    @Column(name = "ordered_qty", nullable = false, precision = 19, scale = 4)
    private BigDecimal orderedQty = BigDecimal.ZERO; // Số lượng đặt mua.

    @Builder.Default
    @Column(name = "received_qty", nullable = false, precision = 19, scale = 4)
    private BigDecimal receivedQty = BigDecimal.ZERO; // Số lượng đã nhập kho.

    @Builder.Default
    @Column(name = "unit_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal unitPrice = BigDecimal.ZERO; // Đơn giá tại thời điểm đặt.

    @Builder.Default
    @Column(name = "tax_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal taxRate = BigDecimal.ZERO; // Tỷ lệ thuế % áp dụng dòng hàng.
}

