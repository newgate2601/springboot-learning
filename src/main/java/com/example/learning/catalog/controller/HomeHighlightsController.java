package com.example.learning.catalog.controller;

import com.example.learning.catalog.dto.HomeBannerResponse;
import com.example.learning.catalog.dto.HomeCategoryResponse;
import com.example.learning.catalog.dto.HomeHighlightsResponse;
import com.example.learning.catalog.dto.HomeProductCardResponse;
import com.example.learning.catalog.service.HomeHighlightsService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/home")
@AllArgsConstructor
@CrossOrigin
public class HomeHighlightsController {
    private final HomeHighlightsService homeHighlightsService;

    @GetMapping("/highlights")
    @Operation(
            summary = "[6.1] Lấy toàn bộ dữ liệu màn Home / Product Highlights",
            description = """
                    Mục 6.1 - API tổng hợp cho màn Home.
                    Trả về banner, danh mục nổi bật, sản phẩm nổi bật, sản phẩm mới và sản phẩm bán chạy.
                    Dùng Pageable qua query params page và size. Sort của màn Home được cố định theo business rule từng section.
                    Banner lấy từ Banner/Campaign module sau này nên hiện trả danh sách rỗng.
                    Trạng thái tồn kho lấy từ Inventory module sau này nên hiện trả UNKNOWN.
                    Sản phẩm bán chạy lấy từ Reporting/Order summary module sau này nên hiện trả danh sách rỗng.
                    """
    )
    public HomeHighlightsResponse getHomeHighlights(@PageableDefault(size = 12) Pageable pageable) {
        return homeHighlightsService.getHomeHighlights(pageable);
    }

    @GetMapping("/highlights/banners")
    @Operation(
            summary = "[6.1] Lấy banner cho màn Home",
            description = """
                    Mục 6.1 - Khu vực banner khuyến mãi/campaign.
                    Dùng Pageable qua query params page và size.
                    Dữ liệu thuộc Banner/Campaign module, chưa thuộc Catalog.
                    API được giữ sẵn để frontend hình dung màn Home, hiện provider mặc định trả danh sách rỗng.
                    """
    )
    public Page<HomeBannerResponse> getBanners(@PageableDefault(size = 5) Pageable pageable) {
        return homeHighlightsService.getBanners(pageable);
    }

    @GetMapping("/highlights/categories")
    @Operation(
            summary = "[6.1] Lấy danh mục nổi bật cho màn Home",
            description = """
                    Mục 6.1 - Khu vực 'Danh mục nổi bật'.
                    Dùng Pageable qua query params page và size.
                    Chỉ trả category ACTIVE và được đánh dấu featured.
                    Entity Category chỉ lưu parentId nếu có cây danh mục, không dùng quan hệ @ManyToOne.
                    """
    )
    public Page<HomeCategoryResponse> getFeaturedCategories(@PageableDefault(size = 8) Pageable pageable) {
        return homeHighlightsService.getFeaturedCategories(pageable);
    }

    @GetMapping("/highlights/featured-products")
    @Operation(
            summary = "[6.1] Lấy sản phẩm nổi bật cho màn Home",
            description = """
                    Mục 6.1 - Khu vực 'Sản phẩm nổi bật'.
                    Dùng Pageable qua query params page và size.
                    Chỉ trả product ACTIVE, category ACTIVE, đã published, được đánh dấu featured và có ít nhất một SKU ACTIVE có giá hợp lệ.
                    Trạng thái tồn kho hiện lấy qua ProductStockProvider; khi có Inventory module thì nối provider này.
                    """
    )
    public Page<HomeProductCardResponse> getFeaturedProducts(@PageableDefault(size = 12) Pageable pageable) {
        return homeHighlightsService.getFeaturedProducts(pageable);
    }

    @GetMapping("/highlights/new-products")
    @Operation(
            summary = "[6.1] Lấy sản phẩm mới cho màn Home",
            description = """
                    Mục 6.1 - Khu vực 'Sản phẩm mới'.
                    Dùng Pageable qua query params page và size.
                    Chỉ trả product ACTIVE, category ACTIVE, đã published và có ít nhất một SKU ACTIVE có giá hợp lệ.
                    Brand, category và SKU được join thủ công bằng các field id trong query, không dùng @ManyToOne.
                    Ảnh chính lấy từ phần tử đầu tiên của product.imageUrls.
                    """
    )
    public Page<HomeProductCardResponse> getNewProducts(@PageableDefault(size = 12) Pageable pageable) {
        return homeHighlightsService.getNewProducts(pageable);
    }

    @GetMapping("/highlights/best-selling-products")
    @Operation(
            summary = "[6.1] Lấy sản phẩm bán chạy cho màn Home",
            description = """
                    Mục 6.1 - Khu vực 'Bán chạy'.
                    Dùng Pageable qua query params page và size.
                    Danh sách product bán chạy nên lấy từ Reporting/Order summary, không query trực tiếp order_items lớn trong Catalog.
                    Hiện tại đã định nghĩa ProductSalesSummaryProvider để module Reporting nối vào sau; provider mặc định trả danh sách rỗng.
                    """
    )
    public Page<HomeProductCardResponse> getBestSellingProducts(@PageableDefault(size = 12) Pageable pageable) {
        return homeHighlightsService.getBestSellingProducts(pageable);
    }
}
