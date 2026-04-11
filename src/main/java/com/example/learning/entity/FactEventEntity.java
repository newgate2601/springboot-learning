package com.example.learning.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "fact_events", schema = "bd")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FactEventEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id")
    private Long eventId;

    @Column(name = "event_ts", nullable = false)
    private OffsetDateTime eventTs;

    @Column(name = "event_type", nullable = false)
    private Short eventType;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "location_id", nullable = false)
    private Long locationId;

    @Column(name = "device_type", nullable = false)
    private Short deviceType;

    @Column(name = "referrer", nullable = false)
    private String referrer;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "properties", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> properties;
}
