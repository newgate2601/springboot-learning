package com.example.learning.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "dim_location", schema = "bd")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DimLocationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "location_id")
    private Long locationId;

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    @Column(name = "region", nullable = false)
    private String region;

    @Column(name = "city", nullable = false)
    private String city;

    @Column(name = "zip", nullable = false)
    private String zip;

    @Column(name = "tz", nullable = false)
    private String tz;

    @Column(name = "lat", nullable = false, precision = 9, scale = 6)
    private BigDecimal lat;

    @Column(name = "lon", nullable = false, precision = 9, scale = 6)
    private BigDecimal lon;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}
