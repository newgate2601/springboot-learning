package com.example.learning.config;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.storage.s3")
public record S3Properties(
        boolean enabled,
        String region,
        String bucket,
        URI endpoint,
        boolean pathStyleAccessEnabled,
        String accessKey,
        String secretKey,
        long presignDurationMinutes
) {
}
