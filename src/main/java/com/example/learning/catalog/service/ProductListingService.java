package com.example.learning.catalog.service;

import com.example.learning.catalog.dto.*;
import com.example.learning.catalog.entity.BrandEntity;
import com.example.learning.catalog.entity.CategoryEntity;
import com.example.learning.catalog.enums.*;
import com.example.learning.catalog.repository.*;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
@AllArgsConstructor
public class ProductListingService {
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 60;
    private static final int MAX_KEYWORD_LENGTH = 100;
    private static final int MAX_BRAND_FILTERS = 50;

    private final ProductListingRepository productListingRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final ProductStockProvider productStockProvider;
    private final ProductSalesSummaryProvider productSalesSummaryProvider;

    @Transactional(readOnly = true)
    public Page<ProductListingCardResponse> search(ProductListingRequest request, Pageable pageable) {
        ProductListingQueryCriteria criteria = normalizeCriteria(request);
        Pageable safePageable = normalizePageable(pageable);
        boolean hasCategoryFilter = criteria.getCategoryIds() != null && !criteria.getCategoryIds().isEmpty();
        boolean hasBrandFilter = criteria.getBrandIds() != null && !criteria.getBrandIds().isEmpty();
        Page<ProductListingProductProjection> products = productListingRepository.search(
                ProductStatus.ACTIVE.name(),
                CategoryStatus.ACTIVE.name(),
                BrandStatus.ACTIVE.name(),
                SkuStatus.ACTIVE.name(),
                criteria.getKeyword() == null ? null : "%" + criteria.getKeyword().toLowerCase(Locale.ROOT) + "%",
                hasCategoryFilter,
                hasCategoryFilter ? criteria.getCategoryIds() : Collections.singletonList(-1L),
                hasBrandFilter,
                hasBrandFilter ? criteria.getBrandIds() : Collections.singletonList(-1L),
                criteria.getMinPrice(),
                criteria.getMaxPrice(),
                criteria.getSort().name(),
                safePageable
        );
        List<ProductListingCardResponse> cards = toProductCards(products.getContent());

        if (criteria.getSort() == ProductListingSort.SOLD_DESC) {
            cards = cards.stream()
                    .sorted(Comparator.comparing(ProductListingCardResponse::getSoldCount).reversed()
                            .thenComparing(ProductListingCardResponse::getProductId, Comparator.reverseOrder()))
                    .collect(Collectors.toList());
        }
        if (criteria.isInStock()) {
            cards = cards.stream()
                    .filter(this::isDisplayInStock)
                    .collect(Collectors.toList());
        }

        return new PageImpl<>(cards, safePageable, products.getTotalElements());
    }

    @Transactional(readOnly = true)
    public ProductListingFiltersResponse getFilterOptions() {
        List<FilterOptionResponse> categories = categoryRepository.findByStatusOrderBySortOrderAscNameAsc(CategoryStatus.ACTIVE)
                .stream()
                .map(category -> FilterOptionResponse.builder()
                        .id(category.getId())
                        .name(category.getName())
                        .slug(category.getSlug())
                        .build())
                .collect(Collectors.toList());
        List<FilterOptionResponse> brands = brandRepository.findByStatusOrderByNameAsc(BrandStatus.ACTIVE)
                .stream()
                .map(brand -> FilterOptionResponse.builder()
                        .id(brand.getId())
                        .name(brand.getName())
                        .slug(brand.getSlug())
                        .build())
                .collect(Collectors.toList());

        return ProductListingFiltersResponse.builder()
                .categories(categories)
                .brands(brands)
                .build();
    }

    private ProductListingQueryCriteria normalizeCriteria(ProductListingRequest request) {
        ProductListingRequest safeRequest = request == null ? new ProductListingRequest() : request;
        String keyword = normalizeKeyword(safeRequest.getKeyword());
        List<Long> categoryIds = resolveCategoryIds(safeRequest);
        List<Long> brandIds = resolveBrandIds(safeRequest.getBrandIds());
        BigDecimal minPrice = normalizePrice(safeRequest.getMinPrice(), "Min price không được âm.");
        BigDecimal maxPrice = normalizePrice(safeRequest.getMaxPrice(), "Max price không được âm.");
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new ResponseStatusException(BAD_REQUEST, "Min price không được lớn hơn max price.");
        }

        return ProductListingQueryCriteria.builder()
                .keyword(keyword)
                .categoryIds(categoryIds)
                .brandIds(brandIds)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .inStock(Boolean.TRUE.equals(safeRequest.getInStock()))
                .sort(ProductListingSort.fromRequestValue(safeRequest.getSort()))
                .build();
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        String trimmedKeyword = keyword.trim();
        if (trimmedKeyword.length() > MAX_KEYWORD_LENGTH) {
            throw new ResponseStatusException(BAD_REQUEST, "Keyword quá dài.");
        }
        return trimmedKeyword;
    }

    private List<Long> resolveCategoryIds(ProductListingRequest request) {
        if (request.getCategoryId() == null && (request.getCategorySlug() == null || request.getCategorySlug().isBlank())) {
            return Collections.emptyList();
        }

        CategoryEntity selectedCategory;
        if (request.getCategoryId() != null) {
            selectedCategory = categoryRepository.findById(request.getCategoryId())
                    .filter(category -> category.getStatus() == CategoryStatus.ACTIVE)
                    .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Category không tồn tại hoặc không active."));
        } else {
            selectedCategory = categoryRepository.findBySlugAndStatus(request.getCategorySlug().trim(), CategoryStatus.ACTIVE)
                    .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Category không tồn tại hoặc không active."));
        }

        List<CategoryEntity> activeCategories = categoryRepository.findByStatusOrderBySortOrderAscNameAsc(CategoryStatus.ACTIVE);
        Map<Long, List<CategoryEntity>> childrenByParentId = activeCategories.stream()
                .filter(category -> category.getParentId() != null)
                .collect(Collectors.groupingBy(CategoryEntity::getParentId));

        List<Long> categoryIds = new ArrayList<>();
        Deque<Long> pending = new ArrayDeque<>();
        pending.add(selectedCategory.getId());
        while (!pending.isEmpty()) {
            Long categoryId = pending.removeFirst();
            categoryIds.add(categoryId);
            childrenByParentId.getOrDefault(categoryId, Collections.emptyList())
                    .forEach(child -> pending.addLast(child.getId()));
        }
        return categoryIds;
    }

    private List<Long> resolveBrandIds(List<Long> rawBrandIds) {
        if (rawBrandIds == null || rawBrandIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> brandIds = rawBrandIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (brandIds.size() > MAX_BRAND_FILTERS) {
            throw new ResponseStatusException(BAD_REQUEST, "Quá nhiều brand filter.");
        }
        if (brandIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<BrandEntity> activeBrands = brandRepository.findByIdInAndStatus(brandIds, BrandStatus.ACTIVE);
        Set<Long> activeBrandIds = activeBrands.stream()
                .map(BrandEntity::getId)
                .collect(Collectors.toSet());
        if (!activeBrandIds.containsAll(brandIds)) {
            throw new ResponseStatusException(BAD_REQUEST, "Brand không tồn tại hoặc không active.");
        }
        return brandIds;
    }

    private BigDecimal normalizePrice(BigDecimal price, String errorMessage) {
        if (price == null) {
            return null;
        }
        if (price.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(BAD_REQUEST, errorMessage);
        }
        return price;
    }

    private List<ProductListingCardResponse> toProductCards(List<ProductListingProductProjection> products) {
        List<Long> productIds = products.stream()
                .map(ProductListingProductProjection::getProductId)
                .collect(Collectors.toList());
        Map<Long, StockDisplayStatus> stockStatusByProductId = productStockProvider.getStockSummaries(productIds)
                .stream()
                .collect(Collectors.toMap(ProductStockSummary::getProductId, ProductStockSummary::getStockStatus));
        Map<Long, Long> soldCountByProductId = productSalesSummaryProvider.getBestSellingProductSummaries(PageRequest.of(0, MAX_PAGE_SIZE))
                .stream()
                .collect(Collectors.toMap(ProductSalesSummary::getProductId, ProductSalesSummary::getSoldCount, Long::max));

        return products.stream()
                .map(product -> ProductListingCardResponse.builder()
                        .productId(product.getProductId())
                        .name(product.getProductName())
                        .slug(product.getSlug())
                        .shortDescription(product.getShortDescription())
                        .brandName(product.getBrandName())
                        .categoryName(product.getCategoryName())
                        .mainImageUrl(getMainImageUrl(product.getImageUrls()))
                        .minSalePrice(product.getMinSalePrice())
                        .minComparePrice(product.getMinComparePrice())
                        .soldCount(soldCountByProductId.getOrDefault(product.getProductId(), 0L))
                        .stockStatus(stockStatusByProductId.getOrDefault(product.getProductId(), StockDisplayStatus.UNKNOWN))
                        .build())
                .collect(Collectors.toList());
    }

    private boolean isDisplayInStock(ProductListingCardResponse product) {
        return product.getStockStatus() == StockDisplayStatus.IN_STOCK
                || product.getStockStatus() == StockDisplayStatus.LOW_STOCK
                || product.getStockStatus() == StockDisplayStatus.UNKNOWN;
    }

    private String getMainImageUrl(String[] imageUrls) {
        if (imageUrls == null || imageUrls.length == 0) {
            return null;
        }
        return imageUrls[0];
    }

    private Pageable normalizePageable(Pageable pageable) {
        if (pageable == null || pageable.isUnpaged()) {
            return PageRequest.of(0, DEFAULT_PAGE_SIZE);
        }
        int pageSize = Math.min(Math.max(pageable.getPageSize(), 1), MAX_PAGE_SIZE);
        return PageRequest.of(Math.max(pageable.getPageNumber(), 0), pageSize);
    }
}
