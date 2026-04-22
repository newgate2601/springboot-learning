package com.example.learning.repository;

import com.example.learning.entity.ExcelImportRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExcelImportRecordRepository extends JpaRepository<ExcelImportRecordEntity, Long> {
}
