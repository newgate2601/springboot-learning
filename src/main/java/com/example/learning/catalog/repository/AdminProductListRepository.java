package com.example.learning.catalog.repository;

import com.example.learning.catalog.entity.ProductEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface AdminProductListRepository extends Repository<ProductEntity, Long> {
    @Query(
            value = """
                    select
                        p.id as "productId",
                        p.name as "productName",
                        p.slug as "slug",
                        p.image_urls as "imageUrls",
                        p.category_id as "categoryId",
                        c.name as "categoryName",
                        p.brand_id as "brandId",
                        b.name as "brandName",
                        p.status as "status",
                        sku_count.total_skus as "skuCount",
                        p.created_at as "createdAt",
                        p.updated_at as "updatedAt",
                        p.published_at as "publishedAt"
                    from catalog_products p
                    join catalog_categories c on c.id = p.category_id
                    left join catalog_brands b on b.id = p.brand_id
                    left join lateral (
                        select count(s.id) as total_skus
                        from catalog_product_skus s
                        where s.product_id = p.id
                    ) sku_count on true
                    where (cast(:status as text) is null or p.status = cast(:status as text))
                      and (
                          cast(:keyword as text) is null
                          or lower(p.name) like cast(:keyword as text)
                          or exists (
                              select 1
                              from catalog_product_skus keyword_sku
                              where keyword_sku.product_id = p.id
                                and lower(keyword_sku.sku_code) like cast(:keyword as text)
                          )
                      )
                    order by p.updated_at desc, p.id desc
                    """,
            countQuery = """
                    select count(p.id)
                    from catalog_products p
                    where (cast(:status as text) is null or p.status = cast(:status as text))
                      and (
                          cast(:keyword as text) is null
                          or lower(p.name) like cast(:keyword as text)
                          or exists (
                              select 1
                              from catalog_product_skus keyword_sku
                              where keyword_sku.product_id = p.id
                                and lower(keyword_sku.sku_code) like cast(:keyword as text)
                          )
                      )
                    """,
            nativeQuery = true
    )
    Page<AdminProductListProjection> search(
            @Param("keyword") String keyword,
            @Param("status") String status,
            Pageable pageable
    );
}
