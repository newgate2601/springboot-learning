package com.example.learning.entity;

import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "phutest")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PhuTestEntity {
    @Id
    private String id;
    private String name;
}
