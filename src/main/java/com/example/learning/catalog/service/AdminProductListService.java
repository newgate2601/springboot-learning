package com.example.learning.catalog.service;

import com.example.learning.catalog.dto.AdminProductListItemResponse;
import com.example.learning.catalog.dto.AdminProductSearchRequest;
import com.example.learning.catalog.enums.ProductStatus;
import com.example.learning.catalog.repository.AdminProductListProjection;
import com.example.learning.catalog.repository.AdminProductListRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
@AllArgsConstructor
public class AdminProductListService {
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_KEYWORD_LENGTH = 100;

    private final AdminProductListRepository adminProductListRepository;

    @Transactional(readOnly = true)
    public Page<AdminProductListItemResponse> search(AdminProductSearchRequest request, Pageable pageable) {
        AdminProductSearchRequest safeRequest = request == null ? new AdminProductSearchRequest() : request;
        String keyword = normalizeKeyword(safeRequest.getKeyword());
        ProductStatus status = normalizeStatus(safeRequest.getStatus());
        Pageable safePageable = normalizePageable(pageable);

        return adminProductListRepository.search(
                keyword == null ? null : "%" + keyword.toLowerCase(Locale.ROOT) + "%",
                status == null ? null : status.name(),
                safePageable
        ).map(this::toResponse);
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        String trimmedKeyword = keyword.trim();
        if (trimmedKeyword.length() > MAX_KEYWORD_LENGTH) {
            throw new ResponseStatusException(BAD_REQUEST, "Keyword qua dai.");
        }
        return trimmedKeyword;
    }

    private ProductStatus normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return ProductStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(BAD_REQUEST, "Status khong hop le.");
        }
    }

    private AdminProductListItemResponse toResponse(AdminProductListProjection product) {
        return AdminProductListItemResponse.builder()
                .productId(product.getProductId())
                .name(product.getProductName())
                .slug(product.getSlug())
                .mainImageUrl(getMainImageUrl(product.getImageUrls()))
                .categoryId(product.getCategoryId())
                .categoryName(product.getCategoryName())
                .brandId(product.getBrandId())
                .brandName(product.getBrandName())
                .status(ProductStatus.valueOf(product.getStatus()))
                .skuCount(product.getSkuCount())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .publishedAt(product.getPublishedAt())
                .build();
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
