package com.example.learning.service.storage;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import com.example.learning.config.S3Properties;
import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
@RequiredArgsConstructor
@ConditionalOnBean(S3Client.class)
public class S3ObjectStorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3Properties s3Properties;
    private final Duration s3PresignDuration;

    public String upload(MultipartFile file, String keyPrefix) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File upload khong duoc de trong.");
        }

        String objectKey = buildObjectKey(keyPrefix, file.getOriginalFilename());
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(s3Properties.bucket())
                .key(objectKey)
                .contentType(file.getContentType())
                .contentLength(file.getSize())
                .build();

        s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        return objectKey;
    }

    public URL generatePresignedUploadUrl(String key, String contentType) {
        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(s3Properties.bucket())
                .key(key)
                .contentType(contentType)
                .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(
                PutObjectPresignRequest.builder()
                        .signatureDuration(s3PresignDuration)
                        .putObjectRequest(objectRequest)
                        .build()
        );
        return presignedRequest.url();
    }

    public URL generatePresignedDownloadUrl(String key) {
        GetObjectRequest objectRequest = GetObjectRequest.builder()
                .bucket(s3Properties.bucket())
                .key(key)
                .build();

        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(
                GetObjectPresignRequest.builder()
                        .signatureDuration(s3PresignDuration)
                        .getObjectRequest(objectRequest)
                        .build()
        );
        return presignedRequest.url();
    }

    public HeadObjectResponse headObject(String key) {
        return s3Client.headObject(
                HeadObjectRequest.builder()
                        .bucket(s3Properties.bucket())
                        .key(key)
                        .build()
        );
    }

    public ResponseInputStream<GetObjectResponse> getObjectStream(String key) {
        return s3Client.getObject(
                GetObjectRequest.builder()
                        .bucket(s3Properties.bucket())
                        .key(key)
                        .build()
        );
    }

    public String generateObjectKey(String keyPrefix, String originalFilename) {
        return buildObjectKey(keyPrefix, originalFilename);
    }

    public void delete(String key) {
        s3Client.deleteObject(
                DeleteObjectRequest.builder()
                        .bucket(s3Properties.bucket())
                        .key(key)
                        .build()
        );
    }

    private String buildObjectKey(String keyPrefix, String originalFilename) {
        String safePrefix = StringUtils.hasText(keyPrefix) ? keyPrefix.trim() : "uploads";
        String safeName = StringUtils.hasText(originalFilename) ? originalFilename.trim() : "file.bin";
        String normalizedName = safeName.replace("\\", "/");
        if (normalizedName.contains("/")) {
            normalizedName = normalizedName.substring(normalizedName.lastIndexOf('/') + 1);
        }
        return safePrefix + "/" + UUID.randomUUID() + "-" + normalizedName;
    }
}
