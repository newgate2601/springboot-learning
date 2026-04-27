package com.example.learning.catalog.entity;

import com.example.learning.catalog.enums.SkuStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "catalog_product_skus")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSkuEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // ID chính của SKU.
    private Long id;

    // ID product mà SKU này thuộc về. Không dùng @ManyToOne, tự join catalog_products bằng product_id khi cần.
    @Column(name = "product_id", nullable = false)
    private Long productId;

    // Mã SKU duy nhất để quản lý bán hàng/kho, ví dụ: "TS-BLK-M".
    @Column(name = "sku_code", nullable = false, unique = true, length = 100)
    private String skuCode;

    // Mã vạch nếu có, phục vụ nghiệp vụ kho/picking/packing sau này.
    @Column(length = 100)
    private String barcode;

    // Nhãn biến thể đơn giản để hiển thị, ví dụ: "Đen / Size M", "128GB / Black".
    @Column(name = "variant_label", length = 255)
    private String variantLabel;

    // Danh sách URL ảnh riêng của SKU. Nếu rỗng/null thì UI có thể dùng imageUrls của product.
    @Column(name = "image_urls", columnDefinition = "text[]")
    private String[] imageUrls;

    // Giá bán hiện tại của SKU. Order sau này phải snapshot giá này tại thời điểm checkout.
    @Column(name = "sale_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal salePrice;

    // Giá so sánh/giá gạch nếu có, ví dụ salePrice 199.000 và comparePrice 249.000.
    @Column(name = "compare_price", precision = 19, scale = 2)
    private BigDecimal comparePrice;

    // Trạng thái SKU: ACTIVE được bán, INACTIVE tạm ngưng, DISCONTINUED ngưng bán lâu dài.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SkuStatus status;

    // Thời điểm tạo SKU.
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // Thời điểm cập nhật SKU gần nhất.
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) {
            status = SkuStatus.ACTIVE;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
