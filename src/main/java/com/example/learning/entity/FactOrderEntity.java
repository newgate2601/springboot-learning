package com.example.learning.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "fact_orders", schema = "bd")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FactOrderEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "order_ts", nullable = false)
    private OffsetDateTime orderTs;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "location_id", nullable = false)
    private Long locationId;

    @Column(name = "qty", nullable = false)
    private Integer qty;

    @Column(name = "unit_price_cents", nullable = false)
    private Integer unitPriceCents;

    @Column(name = "discount_cents", nullable = false)
    private Integer discountCents;

    @Column(name = "shipping_cents", nullable = false)
    private Integer shippingCents;

    @Column(name = "tax_cents", nullable = false)
    private Integer taxCents;

    @Column(name = "status", nullable = false)
    private Short status;

    @Column(name = "payment_method", nullable = false)
    private Short paymentMethod;

    @Column(name = "channel", nullable = false)
    private Short channel;
}
