package com.example.learning.catalog.repository;

import com.example.learning.catalog.enums.CategoryStatus;
import com.example.learning.catalog.entity.CategoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<CategoryEntity, Long> {
    default Page<HomeCategoryProjection> findFeaturedCategories(Pageable pageable) {
        return findFeaturedCategories(CategoryStatus.ACTIVE.name(), pageable);
    }

    @Query(
            value = """
            select
                c.id as "categoryId",
                c.name as name,
                c.slug as slug
            from catalog_categories c
            where c.status = :activeStatus
              and c.featured = true
            order by c.sort_order asc, c.name asc
            """,
            countQuery = """
            select count(c.id)
            from catalog_categories c
            where c.status = :activeStatus
              and c.featured = true
            """,
            nativeQuery = true
    )
    Page<HomeCategoryProjection> findFeaturedCategories(
            @Param("activeStatus") String activeStatus,
            Pageable pageable
    );

    Optional<CategoryEntity> findBySlugAndStatus(String slug, CategoryStatus status);

    List<CategoryEntity> findByStatusOrderBySortOrderAscNameAsc(CategoryStatus status);
}
