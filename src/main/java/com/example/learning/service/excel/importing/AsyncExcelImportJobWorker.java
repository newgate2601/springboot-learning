package com.example.learning.service.excel.importing;

import com.example.learning.entity.AsyncExcelImportJobEntity;
import com.example.learning.entity.AsyncExcelImportJobStatus;
import com.example.learning.entity.ExcelImportRecordEntity;
import com.example.learning.repository.AsyncExcelImportJobRepository;
import com.example.learning.service.storage.S3ObjectStorageService;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.Row;
import org.dhatim.fastexcel.reader.Sheet;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnBean(S3ObjectStorageService.class)
public class AsyncExcelImportJobWorker {

    private static final int CHUNK_SIZE = 5_000;
    private static final String READER_NAME = "fastexcel-s3-async";

    private final AsyncExcelImportJobRepository jobRepository;
    private final ExcelImportRowMapper rowMapper;
    private final ExcelImportPersistenceService persistenceService;
    private final S3ObjectStorageService s3ObjectStorageService;

    @Async("excelImportTaskExecutor")
    public void process(UUID jobId) {
        AsyncExcelImportJobEntity job = getJob(jobId);
        try {
            markRunning(job);
            importFromS3(job);
        } catch (Exception exception) {
            log.error("async excel import failed jobId={}", jobId, exception);
            markFailed(jobId, exception);
        }
    }

    private void importFromS3(AsyncExcelImportJobEntity job) throws IOException {
        if (job.isResetTable()) {
            persistenceService.resetTableForImport(true);
        }

        int processedRows = 0;
        int importedRows = 0;
        try (
                InputStream inputStream = s3ObjectStorageService.getObjectStream(job.getObjectKey());
                ReadableWorkbook workbook = new ReadableWorkbook(inputStream)
        ) {
            Sheet sheet = workbook.getFirstSheet();
            if (sheet == null) {
                throw new IllegalArgumentException("File Excel khong co sheet nao.");
            }

            try (java.util.stream.Stream<Row> stream = sheet.openStream()) {
                Iterator<Row> iterator = stream.iterator();
                if (!iterator.hasNext()) {
                    throw new IllegalArgumentException("File Excel khong co header.");
                }

                Row headerRow = iterator.next();
                rowMapper.validateFastExcelHeader(headerRow);

                List<ExcelImportRecordEntity> chunk = new ArrayList<>(CHUNK_SIZE);
                int displayRowNumber = 2;
                while (iterator.hasNext()) {
                    Row row = iterator.next();
                    if (rowMapper.isFastExcelRowEmpty(row)) {
                        displayRowNumber++;
                        continue;
                    }

                    chunk.add(rowMapper.mapRow(
                            displayRowNumber,
                            rowMapper.getFastExcelCellText(row, 0),
                            rowMapper.getFastExcelCellText(row, 1),
                            rowMapper.getFastExcelCellText(row, 2),
                            rowMapper.getFastExcelCellText(row, 3),
                            rowMapper.getFastExcelCellText(row, 4),
                            rowMapper.getFastExcelCellText(row, 5),
                            rowMapper.getFastExcelCellText(row, 6),
                            rowMapper.getFastExcelCellText(row, 7)
                    ));
                    processedRows++;

                    if (chunk.size() == CHUNK_SIZE) {
                        importedRows += persistenceService.saveChunk(chunk);
                        chunk.clear();
                        updateProgress(job.getId(), processedRows, importedRows);
                    }
                    displayRowNumber++;
                }

                importedRows += persistenceService.saveChunk(chunk);
                updateCompleted(job.getId(), processedRows, importedRows);
            }
        }
    }

    @Transactional
    protected void markRunning(AsyncExcelImportJobEntity job) {
        job.setStatus(AsyncExcelImportJobStatus.RUNNING);
        job.setReader(READER_NAME);
        job.setStartedAt(Instant.now());
        job.setMessage("Dang import file tu S3 bang FastExcel.");
        jobRepository.save(job);
    }

    @Transactional
    protected void updateProgress(UUID jobId, int processedRows, int importedRows) {
        AsyncExcelImportJobEntity job = getJob(jobId);
        job.setProcessedRows(processedRows);
        job.setImportedRows(importedRows);
        job.setFailedRows(processedRows - importedRows);
        job.setMessage("Dang import. Da xu ly " + processedRows + " dong.");
        jobRepository.save(job);
    }

    @Transactional
    protected void updateCompleted(UUID jobId, int processedRows, int importedRows) {
        AsyncExcelImportJobEntity job = getJob(jobId);
        job.setStatus(AsyncExcelImportJobStatus.END);
        job.setProcessedRows(processedRows);
        job.setImportedRows(importedRows);
        job.setFailedRows(processedRows - importedRows);
        job.setFinishedAt(Instant.now());
        job.setMessage("Import thanh cong bang " + READER_NAME + ". So dong da luu: " + importedRows);
        jobRepository.save(job);
    }

    @Transactional
    protected void markFailed(UUID jobId, Exception exception) {
        AsyncExcelImportJobEntity job = getJob(jobId);
        job.setStatus(AsyncExcelImportJobStatus.END);
        job.setFinishedAt(Instant.now());
        job.setMessage(exception.getMessage());
        jobRepository.save(job);
    }

    private AsyncExcelImportJobEntity getJob(UUID jobId) {
        return jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay import job: " + jobId));
    }
}
