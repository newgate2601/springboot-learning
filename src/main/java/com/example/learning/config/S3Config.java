package com.example.learning.config;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@EnableConfigurationProperties(S3Properties.class)
public class S3Config {

    @Bean
    @ConditionalOnProperty(prefix = "app.storage.s3", name = "enabled", havingValue = "true")
    public S3Client s3Client(S3Properties properties) {
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(properties.region()))
                .credentialsProvider(resolveCredentialsProvider(properties))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(properties.pathStyleAccessEnabled())
                        .build());
        applyEndpoint(builder, properties.endpoint());
        return builder.build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.storage.s3", name = "enabled", havingValue = "true")
    public S3Presigner s3Presigner(S3Properties properties) {
        S3Presigner.Builder builder = S3Presigner.builder()
                .region(Region.of(properties.region()))
                .credentialsProvider(resolveCredentialsProvider(properties))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(properties.pathStyleAccessEnabled())
                        .build());
        applyEndpoint(builder, properties.endpoint());
        return builder.build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.storage.s3", name = "enabled", havingValue = "true")
    public Duration s3PresignDuration(S3Properties properties) {
        long durationMinutes = properties.presignDurationMinutes() > 0
                ? properties.presignDurationMinutes()
                : 15;
        return Duration.ofMinutes(durationMinutes);
    }

    private software.amazon.awssdk.auth.credentials.AwsCredentialsProvider resolveCredentialsProvider(
            S3Properties properties
    ) {
        if (StringUtils.hasText(properties.accessKey()) && StringUtils.hasText(properties.secretKey())) {
            return StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(properties.accessKey(), properties.secretKey())
            );
        }
        return DefaultCredentialsProvider.create();
    }

    private void applyEndpoint(S3ClientBuilder builder, URI endpoint) {
        if (endpoint != null) {
            builder.endpointOverride(endpoint);
        }
    }

    private void applyEndpoint(S3Presigner.Builder builder, URI endpoint) {
        if (endpoint != null) {
            builder.endpointOverride(endpoint);
        }
    }
}
