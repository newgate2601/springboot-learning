package com.example.learning.catalog.service;

import com.example.learning.catalog.dto.*;
import com.example.learning.catalog.enums.StockDisplayStatus;
import com.example.learning.catalog.repository.CategoryRepository;
import com.example.learning.catalog.repository.HomeCategoryProjection;
import com.example.learning.catalog.repository.HomeProductProjection;
import com.example.learning.catalog.repository.ProductRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class HomeHighlightsService {
    private static final int DEFAULT_PAGE_SIZE = 12;
    private static final int MAX_PAGE_SIZE = 50;

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final HomeBannerProvider homeBannerProvider;
    private final ProductStockProvider productStockProvider;
    private final ProductSalesSummaryProvider productSalesSummaryProvider;

    @Transactional(readOnly = true)
    public HomeHighlightsResponse getHomeHighlights(Pageable pageable) {
        Pageable safePageable = normalizePageable(pageable);
        return HomeHighlightsResponse.builder()
                .banners(getBanners(safePageable))
                .featuredCategories(getFeaturedCategories(safePageable))
                .featuredProducts(getFeaturedProducts(safePageable))
                .newProducts(getNewProducts(safePageable))
                .bestSellingProducts(getBestSellingProducts(safePageable))
                .build();
    }

    @Transactional(readOnly = true)
    public Page<HomeBannerResponse> getBanners(Pageable pageable) {
        Pageable safePageable = normalizePageable(pageable);
        return homeBannerProvider.getActiveBanners(safePageable);
    }

    @Transactional(readOnly = true)
    public Page<HomeCategoryResponse> getFeaturedCategories(Pageable pageable) {
        return categoryRepository.findFeaturedCategories(normalizePageable(pageable))
                .map(this::toCategoryResponse);
    }

    @Transactional(readOnly = true)
    public Page<HomeProductCardResponse> getNewProducts(Pageable pageable) {
        Page<HomeProductProjection> products = productRepository.findNewestVisibleProducts(normalizePageable(pageable));
        return toProductCards(products, Collections.emptyMap());
    }

    @Transactional(readOnly = true)
    public Page<HomeProductCardResponse> getFeaturedProducts(Pageable pageable) {
        Page<HomeProductProjection> products = productRepository.findFeaturedVisibleProducts(normalizePageable(pageable));
        return toProductCards(products, Collections.emptyMap());
    }

    @Transactional(readOnly = true)
    public Page<HomeProductCardResponse> getBestSellingProducts(Pageable pageable) {
        Pageable safePageable = normalizePageable(pageable);
        Page<ProductSalesSummary> salesSummaries = productSalesSummaryProvider.getBestSellingProductSummaries(safePageable);
        if (salesSummaries.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), safePageable, salesSummaries.getTotalElements());
        }

        List<Long> productIds = salesSummaries.stream()
                .map(ProductSalesSummary::getProductId)
                .collect(Collectors.toList());
        Map<Long, Long> soldCountByProductId = salesSummaries.stream()
                .collect(Collectors.toMap(ProductSalesSummary::getProductId, ProductSalesSummary::getSoldCount));
        Map<Long, Integer> orderByProductId = new HashMap<>();
        for (int i = 0; i < productIds.size(); i++) {
            orderByProductId.put(productIds.get(i), i);
        }

        List<HomeProductProjection> products = productRepository.findVisibleProductsByIds(productIds);
        products.sort(Comparator.comparingInt(product -> orderByProductId.getOrDefault(product.getProductId(), Integer.MAX_VALUE)));
        List<HomeProductCardResponse> cards = toProductCards(products, soldCountByProductId);
        return new PageImpl<>(cards, safePageable, salesSummaries.getTotalElements());
    }

    private Page<HomeProductCardResponse> toProductCards(Page<HomeProductProjection> products, Map<Long, Long> soldCountByProductId) {
        List<HomeProductCardResponse> cards = toProductCards(products.getContent(), soldCountByProductId);
        return new PageImpl<>(cards, products.getPageable(), products.getTotalElements());
    }

    private List<HomeProductCardResponse> toProductCards(List<HomeProductProjection> products, Map<Long, Long> soldCountByProductId) {
        List<Long> productIds = products.stream()
                .map(HomeProductProjection::getProductId)
                .collect(Collectors.toList());
        Map<Long, StockDisplayStatus> stockStatusByProductId = productStockProvider.getStockSummaries(productIds)
                .stream()
                .collect(Collectors.toMap(ProductStockSummary::getProductId, ProductStockSummary::getStockStatus));

        return products.stream()
                .map(product -> toProductCard(product, soldCountByProductId, stockStatusByProductId))
                .collect(Collectors.toList());
    }

    private HomeProductCardResponse toProductCard(
            HomeProductProjection product,
            Map<Long, Long> soldCountByProductId,
            Map<Long, StockDisplayStatus> stockStatusByProductId
    ) {
        return HomeProductCardResponse.builder()
                .productId(product.getProductId())
                .name(product.getProductName())
                .slug(product.getSlug())
                .shortDescription(product.getShortDescription())
                .brandName(product.getBrandName())
                .categoryName(product.getCategoryName())
                .mainImageUrl(getMainImageUrl(product))
                .minSalePrice(product.getMinSalePrice())
                .minComparePrice(product.getMinComparePrice())
                .soldCount(soldCountByProductId.getOrDefault(product.getProductId(), 0L))
                .stockStatus(stockStatusByProductId.getOrDefault(product.getProductId(), StockDisplayStatus.UNKNOWN))
                .build();
    }

    private String getMainImageUrl(HomeProductProjection product) {
        String[] imageUrls = product.getImageUrls();
        if (imageUrls == null || imageUrls.length == 0) {
            return null;
        }
        return imageUrls[0];
    }

    private HomeCategoryResponse toCategoryResponse(HomeCategoryProjection category) {
        return HomeCategoryResponse.builder()
                .categoryId(category.getCategoryId())
                .name(category.getName())
                .slug(category.getSlug())
                .build();
    }

    private Pageable normalizePageable(Pageable pageable) {
        if (pageable == null || pageable.isUnpaged()) {
            return PageRequest.of(0, DEFAULT_PAGE_SIZE);
        }
        int pageSize = Math.min(Math.max(pageable.getPageSize(), 1), MAX_PAGE_SIZE);
        return PageRequest.of(pageable.getPageNumber(), pageSize);
    }
}
