package com.example.learning.catalog.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomeBannerResponse {
    private Long bannerId;
    private String title;
    private String imageUrl;
    private String targetUrl;
}
