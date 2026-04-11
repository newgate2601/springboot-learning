package com.example.learning.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "dim_product", schema = "bd")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DimProductEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long productId;

    @Column(name = "sku", nullable = false)
    private String sku;

    @Column(name = "category_id", nullable = false)
    private Integer categoryId;

    @Column(name = "brand_id", nullable = false)
    private Integer brandId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "price_cents", nullable = false)
    private Integer priceCents;

    @Column(name = "cost_cents", nullable = false)
    private Integer costCents;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
