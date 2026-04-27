package com.example.learning.catalog.service;

import com.example.learning.catalog.dto.HomeBannerResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface HomeBannerProvider {
    Page<HomeBannerResponse> getActiveBanners(Pageable pageable);
}
