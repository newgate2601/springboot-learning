package com.example.learning.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tbl_credit_card")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreditCardEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Integer money;
}
