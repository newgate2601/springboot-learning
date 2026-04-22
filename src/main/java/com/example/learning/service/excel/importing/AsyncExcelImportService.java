package com.example.learning.service.excel.importing;

import com.example.learning.dto.AsyncExcelImportJobResponse;
import com.example.learning.dto.AsyncExcelImportPresignUploadRequest;
import com.example.learning.dto.AsyncExcelImportPresignUploadResponse;
import com.example.learning.entity.AsyncExcelImportJobEntity;
import com.example.learning.entity.AsyncExcelImportJobStatus;
import com.example.learning.repository.AsyncExcelImportJobRepository;
import com.example.learning.service.storage.S3ObjectStorageService;
import java.net.URLConnection;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

@Service
@RequiredArgsConstructor
@ConditionalOnBean(S3ObjectStorageService.class)
public class AsyncExcelImportService {

    private static final String DEFAULT_PREFIX = "excel-import/async";
    private static final String READER_NAME = "fastexcel-s3-async";

    private final AsyncExcelImportJobRepository jobRepository;
    private final AsyncExcelImportJobWorker jobWorker;
    private final S3ObjectStorageService s3ObjectStorageService;
    private final Duration s3PresignDuration;

    public AsyncExcelImportPresignUploadResponse createPresignedUpload(AsyncExcelImportPresignUploadRequest request) {
        String fileName = normalizeFileName(request.fileName());
        validateExcelFileName(fileName);
        String contentType = resolveContentType(request.contentType(), fileName);
        String keyPrefix = StringUtils.hasText(request.keyPrefix()) ? request.keyPrefix().trim() : DEFAULT_PREFIX;
        String objectKey = s3ObjectStorageService.generateObjectKey(keyPrefix, fileName);

        AsyncExcelImportJobEntity job = jobRepository.save(
                AsyncExcelImportJobEntity.builder()
                        .status(AsyncExcelImportJobStatus.NEW)
                        .reader(READER_NAME)
                        .objectKey(objectKey)
                        .fileName(fileName)
                        .contentType(contentType)
                        .resetTable(request.resetTable())
                        .processedRows(0)
                        .importedRows(0)
                        .failedRows(0)
                        .message("Da tao job va cap presigned URL. Hay upload file len S3 roi goi start.")
                        .expiresAt(Instant.now().plus(s3PresignDuration))
                        .build()
        );

        return new AsyncExcelImportPresignUploadResponse(
                job.getId(),
                job.getStatus(),
                objectKey,
                s3ObjectStorageService.generatePresignedUploadUrl(objectKey, contentType).toExternalForm(),
                s3PresignDuration.toMinutes()
        );
    }

    public AsyncExcelImportJobResponse getJob(UUID jobId) {
        return toResponse(jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay import job: " + jobId)));
    }

    @Transactional
    public AsyncExcelImportJobResponse startJob(UUID jobId) {
        AsyncExcelImportJobEntity job = jobRepository.findByIdForUpdate(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay import job: " + jobId));

        validateStartRequest(job);

        HeadObjectResponse objectMetadata = s3ObjectStorageService.headObject(job.getObjectKey());
        validateUploadedObject(job, objectMetadata);

        job.setContentType(objectMetadata.contentType());
        job.setFileSize(objectMetadata.contentLength());
        job.setMessage("Da xac nhan object tren S3. Job se bat dau import.");
        jobRepository.save(job);
        UUID jobIdToProcess = job.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                jobWorker.process(jobIdToProcess);
            }
        });
        return toResponse(job);
    }

    public String generatePresignedSourceDownloadUrl(UUID jobId) {
        AsyncExcelImportJobEntity job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay import job: " + jobId));
        return s3ObjectStorageService.generatePresignedDownloadUrl(job.getObjectKey()).toExternalForm();
    }

    private AsyncExcelImportJobResponse toResponse(AsyncExcelImportJobEntity job) {
        return new AsyncExcelImportJobResponse(
                job.getId(),
                job.getStatus(),
                job.getReader(),
                job.getObjectKey(),
                job.getFileName(),
                job.getContentType(),
                job.getFileSize(),
                job.isResetTable(),
                job.getProcessedRows(),
                job.getImportedRows(),
                job.getFailedRows(),
                job.getMessage(),
                job.getCreatedAt(),
                job.getExpiresAt(),
                job.getStartedAt(),
                job.getFinishedAt(),
                job.getUpdatedAt()
        );
    }

    private String normalizeFileName(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            throw new IllegalArgumentException("fileName khong duoc de trong.");
        }
        return fileName.trim();
    }

    private void validateExcelFileName(String fileName) {
        if (!fileName.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw new IllegalArgumentException("Chi ho tro file .xlsx de import bang FastExcel.");
        }
    }

    private String resolveContentType(String contentType, String fileName) {
        if (StringUtils.hasText(contentType)) {
            return contentType.trim();
        }
        String guessedContentType = URLConnection.guessContentTypeFromName(fileName);
        return guessedContentType != null
                ? guessedContentType
                : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    }

    private void validateStartRequest(AsyncExcelImportJobEntity job) {
        if (job.getStatus() == AsyncExcelImportJobStatus.RUNNING) {
            throw new IllegalStateException("Job da duoc start truoc do. Yeu cau bi tu choi de tranh spam.");
        }
        if (job.getStatus() == AsyncExcelImportJobStatus.END) {
            throw new IllegalStateException("Job da ket thuc, khong the start lai.");
        }
        if (job.getStatus() != AsyncExcelImportJobStatus.NEW) {
            throw new IllegalStateException("Trang thai job khong hop le de start: " + job.getStatus());
        }
        if (job.getExpiresAt() != null && Instant.now().isAfter(job.getExpiresAt())) {
            job.setStatus(AsyncExcelImportJobStatus.END);
            job.setFinishedAt(Instant.now());
            job.setMessage("Job het han truoc khi duoc start.");
            jobRepository.save(job);
            throw new IllegalStateException("Job da het han. Hay tao presigned URL moi.");
        }
    }

    private void validateUploadedObject(AsyncExcelImportJobEntity job, HeadObjectResponse objectMetadata) {
        if (objectMetadata == null || objectMetadata.contentLength() == null || objectMetadata.contentLength() <= 0) {
            throw new IllegalArgumentException("Object tren S3 khong hop le hoac chua upload xong.");
        }

        validateExcelFileName(job.getFileName());
        String actualContentType = objectMetadata.contentType();
        if (StringUtils.hasText(actualContentType)
                && !actualContentType.equalsIgnoreCase("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                && !actualContentType.equalsIgnoreCase("application/octet-stream")
                && !actualContentType.equalsIgnoreCase(job.getContentType())) {
            throw new IllegalArgumentException("Content-Type cua object khong hop le de import Excel: " + actualContentType);
        }
    }
}
