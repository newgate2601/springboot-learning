package com.example.learning.catalog.repository;

import com.example.learning.catalog.enums.CategoryStatus;
import com.example.learning.catalog.entity.CategoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CategoryRepository extends JpaRepository<CategoryEntity, Long> {
    default Page<HomeCategoryProjection> findFeaturedCategories(Pageable pageable) {
        return findFeaturedCategories(CategoryStatus.ACTIVE, pageable);
    }

    @Query(
            value = """
            select
                c.id as categoryId,
                c.name as name,
                c.slug as slug
            from CategoryEntity c
            where c.status = :activeStatus
              and c.featured = true
            order by c.sortOrder asc, c.name asc
            """,
            countQuery = """
            select count(c)
            from CategoryEntity c
            where c.status = :activeStatus
              and c.featured = true
            """
    )
    Page<HomeCategoryProjection> findFeaturedCategories(
            @Param("activeStatus") CategoryStatus activeStatus,
            Pageable pageable
    );
}
