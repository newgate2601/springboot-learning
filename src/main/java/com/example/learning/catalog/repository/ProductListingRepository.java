package com.example.learning.catalog.repository;

import com.example.learning.catalog.entity.ProductEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface ProductListingRepository extends Repository<ProductEntity, Long> {
    @Query(
            value = """
                    select
                        p.id as "productId",
                        p.name as "productName",
                        p.slug as "slug",
                        p.short_description as "shortDescription",
                        b.name as "brandName",
                        c.name as "categoryName",
                        p.image_urls as "imageUrls",
                        sku_price.min_sale_price as "minSalePrice",
                        sku_price.min_compare_price as "minComparePrice",
                        p.published_at as "publishedAt"
                    from catalog_products p
                    join catalog_categories c on c.id = p.category_id
                    left join catalog_brands b on b.id = p.brand_id and b.status = :activeBrandStatus
                    left join lateral (
                        select
                            min(s.sale_price) as min_sale_price,
                            min(s.compare_price) as min_compare_price
                        from catalog_product_skus s
                        where s.product_id = p.id
                          and s.status = :activeSkuStatus
                          and s.sale_price > 0
                    ) sku_price on true
                    where p.status = :activeProductStatus
                      and p.published_at is not null
                      and c.status = :activeCategoryStatus
                      and sku_price.min_sale_price is not null
                      and (
                          cast(:keyword as text) is null
                          or lower(p.name) like cast(:keyword as text)
                          or lower(b.name) like cast(:keyword as text)
                          or exists (
                              select 1
                              from catalog_product_skus keyword_sku
                              where keyword_sku.product_id = p.id
                                and keyword_sku.status = :activeSkuStatus
                                and lower(keyword_sku.sku_code) like cast(:keyword as text)
                          )
                      )
                      and (:categoryFilterEnabled = false or p.category_id in (:categoryIds))
                      and (:brandFilterEnabled = false or p.brand_id in (:brandIds))
                      and (cast(:minPrice as numeric) is null or sku_price.min_sale_price >= cast(:minPrice as numeric))
                      and (cast(:maxPrice as numeric) is null or sku_price.min_sale_price <= cast(:maxPrice as numeric))
                    order by
                      case when :sort = 'PRICE_ASC' then sku_price.min_sale_price end asc,
                      case when :sort = 'PRICE_DESC' then sku_price.min_sale_price end desc,
                      case when :sort = 'NAME_ASC' then lower(p.name) end asc,
                      p.published_at desc,
                      p.id desc
                    """,
            countQuery = """
                    select count(p.id)
                    from catalog_products p
                    join catalog_categories c on c.id = p.category_id
                    left join catalog_brands b on b.id = p.brand_id and b.status = :activeBrandStatus
                    left join lateral (
                        select min(s.sale_price) as min_sale_price
                        from catalog_product_skus s
                        where s.product_id = p.id
                          and s.status = :activeSkuStatus
                          and s.sale_price > 0
                    ) sku_price on true
                    where p.status = :activeProductStatus
                      and p.published_at is not null
                      and c.status = :activeCategoryStatus
                      and sku_price.min_sale_price is not null
                      and (
                          cast(:keyword as text) is null
                          or lower(p.name) like cast(:keyword as text)
                          or lower(b.name) like cast(:keyword as text)
                          or exists (
                              select 1
                              from catalog_product_skus keyword_sku
                              where keyword_sku.product_id = p.id
                                and keyword_sku.status = :activeSkuStatus
                                and lower(keyword_sku.sku_code) like cast(:keyword as text)
                          )
                      )
                      and (:categoryFilterEnabled = false or p.category_id in (:categoryIds))
                      and (:brandFilterEnabled = false or p.brand_id in (:brandIds))
                      and (cast(:minPrice as numeric) is null or sku_price.min_sale_price >= cast(:minPrice as numeric))
                      and (cast(:maxPrice as numeric) is null or sku_price.min_sale_price <= cast(:maxPrice as numeric))
                    """,
            nativeQuery = true
    )
    Page<ProductListingProductProjection> search(
            @Param("activeProductStatus") String activeProductStatus,
            @Param("activeCategoryStatus") String activeCategoryStatus,
            @Param("activeBrandStatus") String activeBrandStatus,
            @Param("activeSkuStatus") String activeSkuStatus,
            @Param("keyword") String keyword,
            @Param("categoryFilterEnabled") boolean categoryFilterEnabled,
            @Param("categoryIds") List<Long> categoryIds,
            @Param("brandFilterEnabled") boolean brandFilterEnabled,
            @Param("brandIds") List<Long> brandIds,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("sort") String sort,
            Pageable pageable
    );
}
