package com.example.learning.repository;

import com.example.learning.entity.PhuTestEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PhuTestRepository extends MongoRepository<PhuTestEntity, String> {
}
