package com.example.learning.mapper;

import com.example.learning.dto.student.StudentCourseMapDto;
import com.example.learning.dto.student.StudentDto;
import com.example.learning.entity.StudentCourseMapEntity;
import com.example.learning.entity.StudentEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface StudentMapper {
    @Mapping(target = "studentCourseMaps", ignore = true)
    StudentEntity getStudentEntity(StudentDto studentDto);

    @Mapping(target = "studentCourseMaps", ignore = true)
    StudentDto getStudentDto(StudentEntity studentEntity);

    StudentCourseMapDto getStudentCourseMapDto(StudentCourseMapEntity studentCourseMapEntity);
}
