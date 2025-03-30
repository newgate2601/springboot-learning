package com.example.learning.service;

import com.example.learning.dto.student.CourseDto;
import com.example.learning.dto.student.StudentCourseMapDto;
import com.example.learning.dto.student.StudentDto;
import com.example.learning.dto.student.StudentPageDto;
import com.example.learning.entity.StudentCourseMapEntity;
import com.example.learning.entity.StudentEntity;
import com.example.learning.mapper.StudentMapper;
import com.example.learning.repository.CustomSpecification;
import com.example.learning.repository.StudentRepository;
import jakarta.persistence.criteria.JoinType;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@AllArgsConstructor
public class LoadStudentService {
    private final StudentRepository studentRepository;
    private final StudentMapper studentMapper;
    private final UpdateStudentService updateStudentService;

    @Transactional(readOnly = true)
    public Page<StudentDto> getStudentBySpecification(StudentPageDto studentPageDto,
                                                      Pageable pageable) {
        Page<StudentEntity> studentEntities = studentRepository.findAll(getSpecification(studentPageDto), pageable);
        if (studentEntities.isEmpty()) {
            return Page.empty();
        }
        return studentEntities.map(studentEntity -> {
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
            return studentDTO;
        });
    }

//    https://chatgpt.com/share/67e964d8-2d8c-8010-ab0a-d933d8f57550

    private Specification<StudentEntity> getSpecification(StudentPageDto studentPageDto) {
//        return CustomSpecification.builder(StudentEntity.class)
//                .search()
//                .isLike("code", studentPageDto.getCode())
//                .isLike("name", studentPageDto.getName())
//
//                .filter()
//                .isEquals("age", studentPageDto.getAge())
//                .isEquals("gender", studentPageDto.getGender())
//                .build();
        return CustomSpecification.<StudentEntity>builder()
                .search()
                .isLike("code", studentPageDto.getCode())
                .isLike("name", studentPageDto.getName())
                .filter()
                .isEquals("age", studentPageDto.getAge())
                .isEquals("gender", studentPageDto.getGender())
                .join(StudentCourseMapEntity.class, "studentCourseMaps", JoinType.INNER)
                .isEqual("score", studentPageDto.getScore())
                .isEqual("grade", studentPageDto.getGrade())
                .build();
    }

//    private Specification<StudentEntity> getSpecification(StudentPageDto studentPageDto) {
//        return (root, query, criteriaBuilder) -> {
//            List<Predicate> orPredicates = new ArrayList<>();
//            List<Predicate> andPredicates = new ArrayList<>();
//
//            // OR: (name LIKE ? OR code LIKE ?)
//            if (Objects.nonNull(studentPageDto.getName())) {
//                orPredicates.add(criteriaBuilder.like(root.get("name"), "%" + studentPageDto.getName() + "%"));
//            }
//            if (Objects.nonNull(studentPageDto.getCode())) {
//                orPredicates.add(criteriaBuilder.like(root.get("code"), "%" + studentPageDto.getCode() + "%"));
//            }
//
//            // AND: (age = ? AND gender = ? AND score >= ?)
//            if (Objects.nonNull(studentPageDto.getAge())) {
//                andPredicates.add(criteriaBuilder.equal(root.get("age"), studentPageDto.getAge()));
//            }
//            if (Objects.nonNull(studentPageDto.getGender())) {
//                andPredicates.add(criteriaBuilder.equal(root.get("gender"), studentPageDto.getGender()));
//            }
//
//            // JOIN với bảng StudentCourseMapEntity (scm)
//            Join<StudentEntity, StudentCourseMapEntity> studentCourseJoin =
//                    root.join("studentCourseMaps", JoinType.INNER);
//            if (Objects.nonNull(studentPageDto.getScore())) {
//                andPredicates.add(criteriaBuilder.greaterThanOrEqualTo(studentCourseJoin.get("score"), studentPageDto.getScore()));
//            }
//
//            Join<StudentCourseMapEntity, CourseEntity> courseJoin = studentCourseJoin.join("course", JoinType.INNER);
//
//            if (Objects.nonNull(studentPageDto.getCourseName())) {
//                andPredicates.add(criteriaBuilder.like(courseJoin.get("name"), "%" + studentPageDto.getCourseName() + "%"));
//            }
//
//            // Combine predicates
//            Predicate orPredicate = orPredicates.isEmpty() ? criteriaBuilder.conjunction() : criteriaBuilder.or(orPredicates.toArray(new Predicate[0]));
//            Predicate andPredicate = andPredicates.isEmpty() ? criteriaBuilder.conjunction() : criteriaBuilder.and(andPredicates.toArray(new Predicate[0]));
//
//            return criteriaBuilder.and(orPredicate, andPredicate);
//        };
//    }

//    private Specification<StudentEntity> getSpecification(StudentPageDto studentPageDto) {
//        Specification<StudentEntity> specification = Specification.where(null);
//
//        // or
//        Specification<StudentEntity> orSpec = Specification.where(null);
//        if (Objects.nonNull(studentPageDto.getName())) {
//            orSpec = orSpec.or((root, query, criteriaBuilder) ->
//                    criteriaBuilder.like(root.get("name"), "%" + studentPageDto.getName() + "%"));
//        }
//        if (Objects.nonNull(studentPageDto.getCode())) {
//            orSpec = orSpec.or((root, query, criteriaBuilder) ->
//                    criteriaBuilder.like(root.get("code"), "%" + studentPageDto.getCode() + "%"));
//        }
//
//        // and
//        Specification<StudentEntity> andSpec = Specification.where(null);
//        if (Objects.nonNull(studentPageDto.getAge())) {
//            andSpec = andSpec.and((root, query, criteriaBuilder) ->
//                    criteriaBuilder.equal(root.get("age"), studentPageDto.getAge()));
//        }
//        if (Objects.nonNull(studentPageDto.getGender())) {
//            andSpec = andSpec.and((root, query, criteriaBuilder) ->
//                    criteriaBuilder.equal(root.get("gender"), studentPageDto.getGender()));
//        }
//
//        return specification.and(andSpec).and(orSpec);
//    }
}
