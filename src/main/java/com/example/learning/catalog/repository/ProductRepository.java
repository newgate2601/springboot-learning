package com.example.learning.catalog.repository;

import com.example.learning.catalog.entity.ProductEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<ProductEntity, Long> {
    default Page<HomeProductProjection> findNewestVisibleProducts(Pageable pageable) {
        return findNewestVisibleProductsQuery(
                ProductVisibilityCriteria.defaultVisible(),
                pageable
        );
    }

    @Query(
            value = """
            select
                p.id as productId,
                p.name as productName,
                p.slug as slug,
                p.shortDescription as shortDescription,
                b.name as brandName,
                c.name as categoryName,
                p.imageUrls as imageUrls,
                (
                    select min(s2.salePrice)
                    from ProductSkuEntity s2
                    where s2.productId = p.id
                      and s2.status = :#{#criteria.activeSkuStatus}
                      and s2.salePrice > :#{#criteria.minSalePrice}
                ) as minSalePrice,
                (
                    select min(s3.comparePrice)
                    from ProductSkuEntity s3
                    where s3.productId = p.id
                      and s3.status = :#{#criteria.activeSkuStatus}
                      and s3.salePrice > :#{#criteria.minSalePrice}
                ) as minComparePrice
            from ProductEntity p
            join CategoryEntity c
              on c.id = p.categoryId
             and c.status = :#{#criteria.activeCategoryStatus}
            left join BrandEntity b
              on b.id = p.brandId
             and b.status = :#{#criteria.activeBrandStatus}
            where p.status = :#{#criteria.activeProductStatus}
              and p.publishedAt is not null
              and exists (
                  select 1
                  from ProductSkuEntity s
                  where s.productId = p.id
                    and s.status = :#{#criteria.activeSkuStatus}
                    and s.salePrice > :#{#criteria.minSalePrice}
              )
            order by p.publishedAt desc, p.id desc
            """,
            countQuery = """
            select count(p.id)
            from ProductEntity p
            join CategoryEntity c
              on c.id = p.categoryId
             and c.status = :#{#criteria.activeCategoryStatus}
            where p.status = :#{#criteria.activeProductStatus}
              and p.publishedAt is not null
              and exists (
                  select 1
                  from ProductSkuEntity s
                  where s.productId = p.id
                    and s.status = :#{#criteria.activeSkuStatus}
                    and s.salePrice > :#{#criteria.minSalePrice}
              )
            """
    )
    Page<HomeProductProjection> findNewestVisibleProductsQuery(
            @Param("criteria") ProductVisibilityCriteria criteria,
            Pageable pageable
    );

    default Page<HomeProductProjection> findFeaturedVisibleProducts(Pageable pageable) {
        return findFeaturedVisibleProductsQuery(
                ProductVisibilityCriteria.defaultVisible(),
                pageable
        );
    }

    @Query(
            value = """
            select
                p.id as productId,
                p.name as productName,
                p.slug as slug,
                p.shortDescription as shortDescription,
                b.name as brandName,
                c.name as categoryName,
                p.imageUrls as imageUrls,
                (
                    select min(s2.salePrice)
                    from ProductSkuEntity s2
                    where s2.productId = p.id
                      and s2.status = :#{#criteria.activeSkuStatus}
                      and s2.salePrice > :#{#criteria.minSalePrice}
                ) as minSalePrice,
                (
                    select min(s3.comparePrice)
                    from ProductSkuEntity s3
                    where s3.productId = p.id
                      and s3.status = :#{#criteria.activeSkuStatus}
                      and s3.salePrice > :#{#criteria.minSalePrice}
                ) as minComparePrice
            from ProductEntity p
            join CategoryEntity c
              on c.id = p.categoryId
             and c.status = :#{#criteria.activeCategoryStatus}
            left join BrandEntity b
              on b.id = p.brandId
             and b.status = :#{#criteria.activeBrandStatus}
            where p.status = :#{#criteria.activeProductStatus}
              and p.publishedAt is not null
              and p.featured = true
              and exists (
                  select 1
                  from ProductSkuEntity s
                  where s.productId = p.id
                    and s.status = :#{#criteria.activeSkuStatus}
                    and s.salePrice > :#{#criteria.minSalePrice}
              )
            order by p.publishedAt desc, p.id desc
            """,
            countQuery = """
            select count(p.id)
            from ProductEntity p
            join CategoryEntity c
              on c.id = p.categoryId
             and c.status = :#{#criteria.activeCategoryStatus}
            where p.status = :#{#criteria.activeProductStatus}
              and p.publishedAt is not null
              and p.featured = true
              and exists (
                  select 1
                  from ProductSkuEntity s
                  where s.productId = p.id
                    and s.status = :#{#criteria.activeSkuStatus}
                    and s.salePrice > :#{#criteria.minSalePrice}
              )
            """
    )
    Page<HomeProductProjection> findFeaturedVisibleProductsQuery(
            @Param("criteria") ProductVisibilityCriteria criteria,
            Pageable pageable
    );

    default List<HomeProductProjection> findVisibleProductsByIds(List<Long> productIds) {
        return findVisibleProductsByIdsQuery(
                productIds,
                ProductVisibilityCriteria.defaultVisible()
        );
    }

    @Query("""
            select
                p.id as productId,
                p.name as productName,
                p.slug as slug,
                p.shortDescription as shortDescription,
                b.name as brandName,
                c.name as categoryName,
                p.imageUrls as imageUrls,
                (
                    select min(s2.salePrice)
                    from ProductSkuEntity s2
                    where s2.productId = p.id
                      and s2.status = :#{#criteria.activeSkuStatus}
                      and s2.salePrice > :#{#criteria.minSalePrice}
                ) as minSalePrice,
                (
                    select min(s3.comparePrice)
                    from ProductSkuEntity s3
                    where s3.productId = p.id
                      and s3.status = :#{#criteria.activeSkuStatus}
                      and s3.salePrice > :#{#criteria.minSalePrice}
                ) as minComparePrice
            from ProductEntity p
            join CategoryEntity c
              on c.id = p.categoryId
             and c.status = :#{#criteria.activeCategoryStatus}
            left join BrandEntity b
              on b.id = p.brandId
             and b.status = :#{#criteria.activeBrandStatus}
            where p.status = :#{#criteria.activeProductStatus}
              and p.publishedAt is not null
              and p.id in (:productIds)
              and exists (
                  select 1
                  from ProductSkuEntity s
                  where s.productId = p.id
                    and s.status = :#{#criteria.activeSkuStatus}
                    and s.salePrice > :#{#criteria.minSalePrice}
              )
            """)
    List<HomeProductProjection> findVisibleProductsByIdsQuery(
            @Param("productIds") List<Long> productIds,
            @Param("criteria") ProductVisibilityCriteria criteria
    );
}
