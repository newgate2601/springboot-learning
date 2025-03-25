package com.example.learning.dto.student;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StudentCourseMapDto {
    private Long id;
    private Integer score;
    private CourseDto course;
}
