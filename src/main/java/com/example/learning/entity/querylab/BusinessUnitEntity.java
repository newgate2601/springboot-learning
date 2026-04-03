package com.example.learning.entity.querylab;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "ql_business_unit",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_ql_business_unit_code", columnNames = "unit_code")
        },
        indexes = {
                @Index(name = "idx_ql_business_unit_active", columnList = "active")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusinessUnitEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // PK của đơn vị kinh doanh.

    @Column(name = "unit_code", nullable = false, length = 30)
    private String unitCode; // Mã đơn vị dùng trong báo cáo và truy vết chứng từ.

    @Column(name = "unit_name", nullable = false, length = 150)
    private String unitName; // Tên hiển thị của đơn vị kinh doanh.

    @Column(name = "country_code", nullable = false, length = 5)
    private String countryCode; // Quốc gia phụ trách để tách dữ liệu theo khu vực.

    @Builder.Default
    @Column(name = "active", nullable = false)
    private Boolean active = true; // Đánh dấu đơn vị còn hoạt động hay đã đóng.
}

