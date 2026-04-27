package com.example.learning.catalog.entity;

import com.example.learning.catalog.enums.BrandStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "catalog_brands")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrandEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // ID chính của thương hiệu.
    private Long id;

    // Tên thương hiệu hiển thị, ví dụ: "Apple", "Nike", "Samsung".
    @Column(nullable = false, length = 255)
    private String name;

    // Chuỗi định danh dùng cho URL/filter, ví dụ: "apple", "nike".
    @Column(nullable = false, unique = true, length = 255)
    private String slug;

    // URL logo thương hiệu, phục vụ màn quản trị brand hoặc filter/listing có logo.
    @Column(name = "logo_url", length = 1000)
    private String logoUrl;

    // Trạng thái brand: ACTIVE thì được chọn cho product, INACTIVE thì không nên dùng cho product mới.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private BrandStatus status;

    // Thời điểm tạo thương hiệu.
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // Thời điểm cập nhật thương hiệu gần nhất.
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) {
            status = BrandStatus.ACTIVE;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
