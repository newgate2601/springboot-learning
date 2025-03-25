package com.example.learning.controller;

import com.example.learning.dto.student.StudentDto;
import com.example.learning.dto.student.StudentPageDto;
import com.example.learning.entity.StudentCourseMapEntity;
import com.example.learning.entity.StudentEntity;
import com.example.learning.service.LoadStudentService;
import com.example.learning.service.UpdateStudentService;
import lombok.AllArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/student")
@AllArgsConstructor
@CrossOrigin
public class StudentController {
    private final UpdateStudentService updateStudentService;
    private final LoadStudentService loadStudentService;

    @GetMapping("spe/list")
    public Page<StudentDto> getStudent(StudentPageDto studentPageDto,
                                       @ParameterObject Pageable pageable) {
        String name = studentPageDto.getName();
        String code = studentPageDto.getCode();
        Integer age = studentPageDto.getAge();

        return loadStudentService.getStudentBySpecification(studentPageDto, pageable);
    }

    @PostMapping
    public void createStudent(@RequestBody StudentDto studentDto) {
        updateStudentService.createStudent(studentDto);
    }

    @GetMapping("/list-with-join-fetch")
    public List<StudentEntity> getStudentsWithJoinFetch() {
        return updateStudentService.getStudentsWithJoinFetch();
    }

    @GetMapping("/list-with-entity-graph")
    public List<StudentEntity> getStudentsWithEntityGraph() {
        return updateStudentService.getStudentsWithEntityGraph();
    }

    @GetMapping("/list")
    public List<StudentDto> getStudents() {
        return updateStudentService.getStudents();
    }

    @GetMapping("/list-with-student")
    public List<StudentCourseMapEntity> findAllWithStudent() {
        return updateStudentService.findAllWithStudent();
    }
}
