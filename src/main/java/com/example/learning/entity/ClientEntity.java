package com.example.learning.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tbl_client")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ClientEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String clientId;
    private String clientSecret;
}
