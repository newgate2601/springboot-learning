package com.example.learning.dto.student;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StudentDto {
    private Long id;
    private String name;
    private String code;
    private Integer age;
    private String gender;
    private List<StudentCourseMapDto> studentCourseMaps;
}
