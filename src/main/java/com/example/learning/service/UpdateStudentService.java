package com.example.learning.service;

import com.example.learning.dto.student.CourseDto;
import com.example.learning.dto.student.StudentCourseMapDto;
import com.example.learning.dto.student.StudentDto;
import com.example.learning.entity.CourseEntity;
import com.example.learning.entity.StudentCourseMapEntity;
import com.example.learning.entity.StudentEntity;
import com.example.learning.mapper.StudentMapper;
import com.example.learning.repository.StudentCourseMapRepository;
import com.example.learning.repository.StudentRepository;
import jakarta.persistence.EntityManager;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@AllArgsConstructor
public class UpdateStudentService {
    private final StudentRepository studentRepository;
    private final StudentCourseMapRepository studentCourseMapRepository;
    private final StudentMapper studentMapper;
    private final EntityManager entityManager;

    @Transactional
    public void createStudent(StudentDto studentDto) {
        StudentEntity studentEntity = studentMapper.getStudentEntity(studentDto);

        List<StudentCourseMapEntity> studentCourseMapEntities = new ArrayList<>();

        for (StudentCourseMapDto studentCourseMapDto : studentDto.getStudentCourseMaps()) {
            StudentCourseMapEntity studentCourseMapEntity = StudentCourseMapEntity.builder()
                    .id(studentCourseMapDto.getId())
                    .student(studentEntity)
                    .score(studentCourseMapDto.getScore())
                    .build();

            studentCourseMapEntity.setCourse(entityManager.getReference(CourseEntity.class, studentCourseMapDto
                    .getCourse().getId()));

            studentCourseMapEntities.add(studentCourseMapEntity);
        }
        studentEntity.setStudentCourseMaps(studentCourseMapEntities);

        studentRepository.save(studentEntity);
    }

    @Transactional(readOnly = true)
    public List<StudentEntity> getStudentsWithJoinFetch() {
        return studentRepository.findAllWithCourses();
    }

    @Transactional(readOnly = true)
    public List<StudentEntity> getStudentsWithEntityGraph() {
        return studentRepository.findAllWithEntityGraph();
    }

    @Transactional(readOnly = true)
    public List<StudentDto> getStudents(){
        List<StudentEntity> studentEntities = studentRepository.findAll();
        return mapToDto(studentEntities);
    }

    public List<StudentDto> mapToDto(List<StudentEntity> studentEntities) {
        List<StudentDto> studentDTOs = new ArrayList<>();
        for (StudentEntity studentEntity : studentEntities) {
            StudentDto studentDTO = studentMapper.getStudentDto(studentEntity);
            List<StudentCourseMapDto> studentCourseMapDTOs = new ArrayList<>();
            List<StudentCourseMapEntity> studentCourseMapEntities = studentEntity.getStudentCourseMaps();
            if (Objects.nonNull(studentCourseMapEntities)) {
                for (StudentCourseMapEntity studentCourseMapEntity : studentCourseMapEntities) {
                    studentCourseMapDTOs.add(
                            StudentCourseMapDto.builder()
                                    .id(studentCourseMapEntity.getId())
                                    .score(studentCourseMapEntity.getScore())
                                    .course(
                                            CourseDto.builder()
                                                    .id(studentCourseMapEntity.getCourse().getId())
                                                    .name(studentCourseMapEntity.getCourse().getName())
                                                    .build()
                                    )
                                    .build()
                    );
                }
            }
            studentDTO.setStudentCourseMaps(studentCourseMapDTOs);
            studentDTOs.add(studentDTO);
        }
        return studentDTOs;
    }

    @Transactional(readOnly = true)
    public List<StudentCourseMapEntity> findAllWithStudent(){
        return studentCourseMapRepository.findAllWithStudent();
    }
}
