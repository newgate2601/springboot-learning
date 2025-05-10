package com.example.learning.entity;

import jakarta.persistence.Id;
import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document(collection = "post")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PostEntity {
    @Id
    private String id;
    private String name;
    private String code;
    private List<PostMapping> postMappings;
}
