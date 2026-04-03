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
        name = "ql_sales_order",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_ql_sales_order_no", columnNames = "so_number")
        },
        indexes = {
                @Index(name = "idx_ql_sales_order_customer", columnList = "customer_id"),
                @Index(name = "idx_ql_sales_order_bu", columnList = "business_unit_id"),
                @Index(name = "idx_ql_sales_order_status", columnList = "status"),
                @Index(name = "idx_ql_sales_order_order_date", columnList = "order_date")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesOrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // PK của đơn bán hàng.

    @Column(name = "so_number", nullable = false, length = 50)
    private String soNumber; // Mã SO nghiệp vụ.

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false, foreignKey = @ForeignKey(name = "fk_ql_sales_order_customer"))
    private CustomerEntity customer; // Khách hàng đặt mua.

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_unit_id", nullable = false, foreignKey = @ForeignKey(name = "fk_ql_sales_order_bu"))
    private BusinessUnitEntity businessUnit; // Đơn vị bán phụ trách đơn hàng.

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sales_owner_user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_ql_sales_order_user"))
    private UserAccountEntity salesOwner; // Nhân sự kinh doanh quản lý đơn.

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate; // Ngày tạo đơn bán.

    @Column(name = "requested_ship_date")
    private LocalDate requestedShipDate; // Ngày KH mong muốn giao.

    @Column(name = "status", nullable = false, length = 30)
    private String status; // Trạng thái: NEW, ALLOCATED, SHIPPED, COMPLETED.

    @Builder.Default
    @Column(name = "total_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO; // Tổng giá trị đơn hàng.

    @Builder.Default
    @Column(name = "currency_code", nullable = false, length = 5)
    private String currencyCode = "USD"; // Tiền tệ bán hàng.
}

