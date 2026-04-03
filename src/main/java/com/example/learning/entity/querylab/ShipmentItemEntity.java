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
        name = "ql_shipment_item",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_ql_shipment_item_unique",
                        columnNames = {"shipment_id", "sales_order_line_id", "inventory_lot_id"}
                )
        },
        indexes = {
                @Index(name = "idx_ql_shipment_item_shipment", columnList = "shipment_id"),
                @Index(name = "idx_ql_shipment_item_so_line", columnList = "sales_order_line_id"),
                @Index(name = "idx_ql_shipment_item_lot", columnList = "inventory_lot_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipmentItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // PK của dòng chi tiết shipment.

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipment_id", nullable = false, foreignKey = @ForeignKey(name = "fk_ql_shipment_item_shipment"))
    private ShipmentEntity shipment; // Header shipment chứa dòng này.

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sales_order_line_id", nullable = false, foreignKey = @ForeignKey(name = "fk_ql_shipment_item_so_line"))
    private SalesOrderLineEntity salesOrderLine; // Dòng SO được giao.

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventory_lot_id", nullable = false, foreignKey = @ForeignKey(name = "fk_ql_shipment_item_lot"))
    private InventoryLotEntity inventoryLot; // Lô tồn kho xuất cho dòng giao hàng.

    @Builder.Default
    @Column(name = "shipped_qty", nullable = false, precision = 19, scale = 4)
    private BigDecimal shippedQty = BigDecimal.ZERO; // Số lượng thực tế đã giao.
}

