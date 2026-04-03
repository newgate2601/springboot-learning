package com.example.learning.entity.querylab;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(
        name = "ql_shipment",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_ql_shipment_no", columnNames = "shipment_number")
        },
        indexes = {
                @Index(name = "idx_ql_shipment_so", columnList = "sales_order_id"),
                @Index(name = "idx_ql_shipment_wh", columnList = "warehouse_id"),
                @Index(name = "idx_ql_shipment_status", columnList = "status"),
                @Index(name = "idx_ql_shipment_shipped_date", columnList = "shipped_date")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // PK của phiếu giao hàng.

    @Column(name = "shipment_number", nullable = false, length = 50)
    private String shipmentNumber; // Mã shipment để theo dõi vận đơn.

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sales_order_id", nullable = false, foreignKey = @ForeignKey(name = "fk_ql_shipment_so"))
    private SalesOrderEntity salesOrder; // SO được giao trong shipment này.

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false, foreignKey = @ForeignKey(name = "fk_ql_shipment_wh"))
    private WarehouseEntity warehouse; // Kho xuất hàng.

    @Column(name = "shipped_date", nullable = false)
    private LocalDate shippedDate; // Ngày thực tế xuất kho.

    @Column(name = "carrier_name", length = 100)
    private String carrierName; // Đơn vị vận chuyển.

    @Column(name = "tracking_number", length = 100)
    private String trackingNumber; // Mã tracking từ carrier.

    @Column(name = "status", nullable = false, length = 30)
    private String status; // Trạng thái: PICKING, IN_TRANSIT, DELIVERED.
}

