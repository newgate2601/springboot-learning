package com.example.learning.catalog.entity;

import com.example.learning.catalog.enums.CategoryStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "catalog_categories")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // ID chính của danh mục.
    private Long id;

    // Tên danh mục hiển thị cho customer/admin, ví dụ: "Áo thun", "Điện thoại".
    @Column(nullable = false, length = 255)
    private String name;

    // Chuỗi định danh dùng cho URL/filter, ví dụ: "ao-thun", "dien-thoai".
    @Column(nullable = false, unique = true, length = 255)
    private String slug;

    // ID danh mục cha. Null nghĩa là danh mục gốc. Không dùng @ManyToOne, tự join bằng parent_id khi cần.
    @Column(name = "parent_id")
    private Long parentId;

    // Trạng thái danh mục: ACTIVE thì customer có thể thấy, INACTIVE thì không nên hiển thị public.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CategoryStatus status;

    // Có được hiển thị ở khu vực "Danh mục nổi bật" trên màn Home 6.1 hay không.
    @Column(nullable = false)
    private Boolean featured;

    // Thứ tự hiển thị danh mục nổi bật hoặc menu danh mục, số nhỏ hiển thị trước.
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    // Thời điểm tạo danh mục.
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // Thời điểm cập nhật danh mục gần nhất.
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) {
            status = CategoryStatus.ACTIVE;
        }
        if (featured == null) {
            featured = false;
        }
        if (sortOrder == null) {
            sortOrder = 0;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
