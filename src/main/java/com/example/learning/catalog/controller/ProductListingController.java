package com.example.learning.catalog.controller;

import com.example.learning.catalog.dto.*;
import com.example.learning.catalog.service.ProductListingService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/catalog/products")
@AllArgsConstructor
@CrossOrigin
public class ProductListingController {
    private final ProductListingService productListingService;

    @GetMapping
    @Operation(
            summary = "[6.2] Tìm kiếm và lọc sản phẩm cho màn Product Listing",
            description = """
                    Mục 6.2 - Product Listing cho customer.
                    Chỉ trả product ACTIVE, category ACTIVE, đã published và có ít nhất một SKU ACTIVE có giá > 0.
                    Keyword tìm theo tên product, tên brand và SKU code active.
                    Category cha sẽ bao gồm product thuộc các category con active.
                    Price range dựa trên giá SKU active thấp nhất của product.
                    inStock=true hiện được xử lý qua ProductStockProvider.
                    provider mặc định trả UNKNOWN cho tới khi nối Inventory.
                    Sort hỗ trợ: newest, price_asc, price_desc, name_asc.
                    sold_desc. sold_desc sẽ dùng summary khi Reporting provider sẵn sàng.
                    """
    )
    public Page<ProductListingCardResponse> search(
            @ModelAttribute ProductListingRequest request,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return productListingService.search(request, pageable);
    }

    @GetMapping("/filters")
    @Operation(
            summary = "[6.2] Lấy dữ liệu bộ lọc Product Listing",
            description = """
                    Trả về danh sách category ACTIVE và brand ACTIVE để frontend render filter checkbox.
                    Category trả flat list, frontend có thể dùng slug/id để reload listing.
                    """
    )
    public ProductListingFiltersResponse getFilterOptions() {
        return productListingService.getFilterOptions();
    }
}
