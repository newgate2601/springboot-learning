package com.example.learning.entity.querylab;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "ql_product_category",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_ql_product_category_code", columnNames = "category_code")
        },
        indexes = {
                @Index(name = "idx_ql_product_category_parent", columnList = "parent_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductCategoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // PK của danh mục sản phẩm.

    @Column(name = "category_code", nullable = false, length = 40)
    private String categoryCode; // Mã danh mục để gom nhóm báo cáo.

    @Column(name = "category_name", nullable = false, length = 200)
    private String categoryName; // Tên danh mục hiển thị cho người dùng.

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", foreignKey = @ForeignKey(name = "fk_ql_product_category_parent"))
    private ProductCategoryEntity parent; // Danh mục cha để tạo cây category.
}

