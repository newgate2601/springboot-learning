package com.example.learning.catalog.service;

import com.example.learning.catalog.dto.HomeBannerResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class NoopHomeBannerProvider implements HomeBannerProvider {
    @Override
    public Page<HomeBannerResponse> getActiveBanners(Pageable pageable) {
        return new PageImpl<>(Collections.emptyList(), pageable, 0);
    }
}
