package com.example.learning.entity.product;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tbl_product_mapping")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductMappingEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String productCode;
    private String partCode;
    private String po;

    private String pcOrder;
}
