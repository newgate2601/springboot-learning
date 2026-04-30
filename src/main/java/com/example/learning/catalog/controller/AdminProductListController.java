package com.example.learning.catalog.controller;

import com.example.learning.catalog.dto.AdminProductListItemResponse;
import com.example.learning.catalog.dto.AdminProductSearchRequest;
import com.example.learning.catalog.service.AdminProductListService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/catalog/products")
@AllArgsConstructor
@CrossOrigin
public class AdminProductListController {
    private final AdminProductListService adminProductListService;

    @GetMapping
    @Operation(
            summary = "[7.1] Search products for Admin Product List",
            description = """
                    Admin product list search.
                    Keyword filters by product name or SKU code.
                    Status filters DRAFT, ACTIVE, INACTIVE, ARCHIVED.
                    Admin can see every product status; public product visibility rules are not applied here.
                    """
    )
    public Page<AdminProductListItemResponse> search(
            @ModelAttribute AdminProductSearchRequest request,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return adminProductListService.search(request, pageable);
    }
}
