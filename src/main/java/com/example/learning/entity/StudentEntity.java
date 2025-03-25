package com.example.learning.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "tbl_student")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StudentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String code;

    private Integer age;

    private String gender;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "student", orphanRemoval = true)
    @JsonIgnoreProperties("student")
    private List<StudentCourseMapEntity> studentCourseMaps;
}
