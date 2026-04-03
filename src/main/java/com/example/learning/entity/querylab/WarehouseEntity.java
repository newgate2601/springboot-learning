package com.example.learning.entity.querylab;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "ql_warehouse",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_ql_warehouse_code", columnNames = "warehouse_code")
        },
        indexes = {
                @Index(name = "idx_ql_warehouse_bu", columnList = "business_unit_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarehouseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // PK của kho.

    @Column(name = "warehouse_code", nullable = false, length = 40)
    private String warehouseCode; // Mã kho dùng trên phiếu xuất/nhập.

    @Column(name = "warehouse_name", nullable = false, length = 150)
    private String warehouseName; // Tên kho hiển thị trong màn hình vận hành.

    @Column(name = "region", nullable = false, length = 60)
    private String region; // Vùng địa lý để tối ưu route giao hàng.

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_unit_id", nullable = false, foreignKey = @ForeignKey(name = "fk_ql_warehouse_bu"))
    private BusinessUnitEntity businessUnit; // Đơn vị quản lý kho này.
}

