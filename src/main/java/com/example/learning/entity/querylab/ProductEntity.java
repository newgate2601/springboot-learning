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
        name = "ql_product",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_ql_product_sku", columnNames = "sku")
        },
        indexes = {
                @Index(name = "idx_ql_product_category", columnList = "category_id"),
                @Index(name = "idx_ql_product_active", columnList = "active")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // PK của sản phẩm.

    @Column(name = "sku", nullable = false, length = 60)
    private String sku; // SKU duy nhất dùng để join giữa mua, bán và tồn kho.

    @Column(name = "product_name", nullable = false, length = 255)
    private String productName; // Tên sản phẩm hiển thị trên chứng từ.

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false, foreignKey = @ForeignKey(name = "fk_ql_product_category"))
    private ProductCategoryEntity category; // Nhóm sản phẩm để query theo ngành hàng.

    @Column(name = "uom", nullable = false, length = 20)
    private String uom; // Đơn vị tính chuẩn như PCS, BOX, KG.

    @Builder.Default
    @Column(name = "standard_cost", nullable = false, precision = 18, scale = 2)
    private BigDecimal standardCost = BigDecimal.ZERO; // Giá vốn tham chiếu khi tính biên lợi nhuận.

    @Builder.Default
    @Column(name = "active", nullable = false)
    private Boolean active = true; // Đánh dấu sản phẩm còn được bán hay không.
}

