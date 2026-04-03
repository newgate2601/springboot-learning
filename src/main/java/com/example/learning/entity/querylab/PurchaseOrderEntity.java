package com.example.learning.entity.querylab;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
        name = "ql_purchase_order",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_ql_purchase_order_no", columnNames = "po_number")
        },
        indexes = {
                @Index(name = "idx_ql_purchase_order_supplier", columnList = "supplier_id"),
                @Index(name = "idx_ql_purchase_order_bu", columnList = "business_unit_id"),
                @Index(name = "idx_ql_purchase_order_status", columnList = "status"),
                @Index(name = "idx_ql_purchase_order_order_date", columnList = "order_date")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // PK của phiếu mua hàng.

    @Column(name = "po_number", nullable = false, length = 50)
    private String poNumber; // Mã PO nghiệp vụ để truy vết giao dịch.

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false, foreignKey = @ForeignKey(name = "fk_ql_purchase_order_supplier"))
    private SupplierEntity supplier; // NCC nhận PO.

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_unit_id", nullable = false, foreignKey = @ForeignKey(name = "fk_ql_purchase_order_bu"))
    private BusinessUnitEntity businessUnit; // Đơn vị tạo PO.

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ordered_by_user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_ql_purchase_order_user"))
    private UserAccountEntity orderedBy; // User chịu trách nhiệm đặt mua.

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate; // Ngày đặt mua.

    @Column(name = "expected_delivery_date")
    private LocalDate expectedDeliveryDate; // Ngày dự kiến nhận hàng.

    @Column(name = "status", nullable = false, length = 30)
    private String status; // Trạng thái PO: DRAFT, APPROVED, RECEIVING, CLOSED.

    @Builder.Default
    @Column(name = "total_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO; // Tổng tiền trước đối soát thanh toán.

    @Builder.Default
    @Column(name = "currency_code", nullable = false, length = 5)
    private String currencyCode = "USD"; // Tiền tệ của PO.
}

