package com.example.learning.dto.student;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StudentPageDto {
    private String name;
    private String code;
    private Integer age;
    private String gender;
    private Integer score;
    private String courseName;
}
