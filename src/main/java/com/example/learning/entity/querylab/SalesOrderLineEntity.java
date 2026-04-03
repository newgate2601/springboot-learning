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
        name = "ql_sales_order_line",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_ql_so_line_no", columnNames = {"sales_order_id", "line_no"})
        },
        indexes = {
                @Index(name = "idx_ql_so_line_so", columnList = "sales_order_id"),
                @Index(name = "idx_ql_so_line_product", columnList = "product_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesOrderLineEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // PK của dòng chi tiết SO.

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sales_order_id", nullable = false, foreignKey = @ForeignKey(name = "fk_ql_so_line_so"))
    private SalesOrderEntity salesOrder; // Header SO chứa dòng này.

    @Column(name = "line_no", nullable = false)
    private Integer lineNo; // Số thứ tự dòng trong SO.

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false, foreignKey = @ForeignKey(name = "fk_ql_so_line_product"))
    private ProductEntity product; // Sản phẩm được bán.

    @Builder.Default
    @Column(name = "ordered_qty", nullable = false, precision = 19, scale = 4)
    private BigDecimal orderedQty = BigDecimal.ZERO; // Số lượng KH đặt mua.

    @Builder.Default
    @Column(name = "allocated_qty", nullable = false, precision = 19, scale = 4)
    private BigDecimal allocatedQty = BigDecimal.ZERO; // Số lượng đã giữ tồn kho.

    @Builder.Default
    @Column(name = "unit_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal unitPrice = BigDecimal.ZERO; // Đơn giá bán cho KH.

    @Builder.Default
    @Column(name = "discount_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal discountRate = BigDecimal.ZERO; // Tỷ lệ giảm giá % trên dòng bán.
}

