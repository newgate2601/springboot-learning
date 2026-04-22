package com.example.learning.repository;

import com.example.learning.entity.AsyncExcelImportJobEntity;
import jakarta.persistence.LockModeType;
import java.util.UUID;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AsyncExcelImportJobRepository extends JpaRepository<AsyncExcelImportJobEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select job from AsyncExcelImportJobEntity job where job.id = :id")
    java.util.Optional<AsyncExcelImportJobEntity> findByIdForUpdate(UUID id);
}
