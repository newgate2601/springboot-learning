package com.example.learning.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tbl_post")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PostEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email")
    private String email;
}
