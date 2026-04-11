package com.example.learning.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "dim_customer", schema = "bd")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DimCustomerEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "customer_uuid", nullable = false)
    private UUID customerUuid;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "gender", nullable = false, length = 1)
    private String gender;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(name = "signup_at", nullable = false)
    private OffsetDateTime signupAt;

    @Column(name = "status", nullable = false)
    private Short status;

    @Column(name = "segment", nullable = false)
    private Short segment;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
