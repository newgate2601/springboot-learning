package com.example.learning.repository;

import com.example.learning.entity.StudentEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentRepository extends JpaRepository<StudentEntity, Long>,
        JpaSpecificationExecutor<StudentEntity> {
    @Query("SELECT s FROM StudentEntity s LEFT JOIN FETCH s.studentCourseMaps scm LEFT JOIN FETCH scm.course")
    List<StudentEntity> findAllWithCourses();

    @EntityGraph(attributePaths = {
            "studentCourseMaps"
//                    , "bổ sung field nếu có thêm relationship"
    }
    )
    @Query("SELECT s FROM StudentEntity s")
    List<StudentEntity> findAllWithEntityGraph();
}
