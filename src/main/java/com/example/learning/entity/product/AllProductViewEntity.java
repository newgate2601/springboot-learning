package com.example.learning.entity.product;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.Subselect;
import org.springframework.data.annotation.Immutable;

@Entity
@Table(name = "all_product_view")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Immutable // spring know that a View, not a table -> no auto create table + no create, update in this view
//@Subselect("SELECT id, product_code, part_code, po, pc_order FROM tbl_product_mapping " +
//        "UNION " +
//        "SELECT id, product_code, part_code, po, pc_order FROM tbl_product_part")
//@Subselect("SELECT * FROM all_product_view")
public class AllProductViewEntity {
    @Id
    private String id;

    private String productCode;
    private String partCode;
    private String po;

    private String pcOrder;
}
