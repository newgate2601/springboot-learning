package com.example.learning.catalog.entity;

import com.example.learning.catalog.enums.ProductStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "catalog_products")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // ID chính của product.
    private Long id;

    // Tên sản phẩm ở mức tổng quát, ví dụ: "Áo thun nam basic".
    @Column(nullable = false, length = 255)
    private String name;

    // Chuỗi định danh dùng cho URL detail, ví dụ: "ao-thun-nam-basic".
    @Column(nullable = false, unique = true, length = 255)
    private String slug;

    // Mô tả ngắn dùng ở listing/card hoặc phần đầu màn detail.
    @Column(name = "short_description", length = 1000)
    private String shortDescription;

    // Mô tả chi tiết sản phẩm, có thể dài và chứa nhiều thông tin nghiệp vụ/marketing.
    @Column(name = "description", columnDefinition = "text")
    private String description;

    // Danh sách URL ảnh của product. Phần tử đầu tiên được xem là ảnh chính cho Home/listing.
    @Column(name = "image_urls", columnDefinition = "text[]")
    private String[] imageUrls;

    // ID thương hiệu của product. Không dùng @ManyToOne, tự join catalog_brands bằng brand_id khi cần.
    @Column(name = "brand_id")
    private Long brandId;

    // ID danh mục chính của product. Không dùng @ManyToOne, tự join catalog_categories bằng category_id khi cần.
    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    // Trạng thái product: DRAFT, ACTIVE, INACTIVE, ARCHIVED.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ProductStatus status;

    // Có được đưa vào khu vực "Sản phẩm nổi bật" trên màn Home 6.1 hay không.
    @Column(nullable = false)
    private Boolean featured;

    // Thời điểm product được publish. Dùng để sort khu vực "Sản phẩm mới" trên Home 6.1.
    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    // Thời điểm tạo product.
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // Thời điểm cập nhật product gần nhất.
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) {
            status = ProductStatus.DRAFT;
        }
        if (featured == null) {
            featured = false;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
