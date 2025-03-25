package com.example.learning.repository;

import com.example.learning.entity.StudentCourseMapEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentCourseMapRepository extends JpaRepository<StudentCourseMapEntity, Long> {
    @Query("SELECT s FROM StudentCourseMapEntity s LEFT JOIN FETCH s.student")
    List<StudentCourseMapEntity> findAllWithStudent();
}
