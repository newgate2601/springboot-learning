package com.example.learning.service.excel.importing;

import com.example.learning.dto.ExcelImportResult;
import com.example.learning.entity.ExcelImportRecordEntity;
import com.example.learning.repository.ExcelImportRecordRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Slf4j
@RequiredArgsConstructor
public class ExcelImportPersistenceService {

    private final ExcelImportRecordRepository excelImportRecordRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public void resetTableForImport(boolean resetTable) {
        if (resetTable) {
            excelImportRecordRepository.deleteAllInBatch();
            excelImportRecordRepository.flush();
        }
    }

    @Transactional
    public int saveChunk(List<ExcelImportRecordEntity> rows) {
        if (rows.isEmpty()) {
            return 0;
        }

        excelImportRecordRepository.saveAll(rows);
        excelImportRecordRepository.flush();
        entityManager.clear();
        return rows.size();
    }

    public ExcelImportResult buildResult(
            String readerName,
            MultipartFile file,
            boolean resetTable,
            int importedRows
    ) {
        ExcelImportResult result = ExcelImportResult.builder()
                .reader(readerName)
                .fileName(file.getOriginalFilename())
                .importedRows(importedRows)
                .resetTable(resetTable)
                .message("Import thanh cong bang " + readerName + ". So dong da luu: " + importedRows)
                .build();

        log.info(
                "excel-import completed reader={} file={} rows={} resetTable={}",
                result.getReader(),
                result.getFileName(),
                result.getImportedRows(),
                result.isResetTable()
        );

        return result;
    }
}
