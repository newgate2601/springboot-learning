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
        name = "ql_inventory_lot",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_ql_inventory_lot", columnNames = {"warehouse_id", "product_id", "lot_code"})
        },
        indexes = {
                @Index(name = "idx_ql_inventory_lot_wh", columnList = "warehouse_id"),
                @Index(name = "idx_ql_inventory_lot_product", columnList = "product_id"),
                @Index(name = "idx_ql_inventory_lot_expiry", columnList = "expiry_date")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryLotEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // PK của lô tồn kho.

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false, foreignKey = @ForeignKey(name = "fk_ql_inventory_lot_warehouse"))
    private WarehouseEntity warehouse; // Kho đang chứa lô này.

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false, foreignKey = @ForeignKey(name = "fk_ql_inventory_lot_product"))
    private ProductEntity product; // Sản phẩm của lô tồn.

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_po_line_id", foreignKey = @ForeignKey(name = "fk_ql_inventory_lot_source_po_line"))
    private PurchaseOrderLineEntity sourcePoLine; // Dòng PO tạo ra lô tồn (traceability).

    @Column(name = "lot_code", nullable = false, length = 80)
    private String lotCode; // Mã lô phục vụ FEFO/FIFO.

    @Builder.Default
    @Column(name = "on_hand_qty", nullable = false, precision = 19, scale = 4)
    private BigDecimal onHandQty = BigDecimal.ZERO; // Tồn thực tế đang có.

    @Builder.Default
    @Column(name = "reserved_qty", nullable = false, precision = 19, scale = 4)
    private BigDecimal reservedQty = BigDecimal.ZERO; // Tồn đã giữ cho đơn bán.

    @Column(name = "expiry_date")
    private LocalDate expiryDate; // Hạn sử dụng cho planning xuất hàng.

    @Column(name = "received_date", nullable = false)
    private LocalDate receivedDate; // Ngày lô được nhập kho.
}

