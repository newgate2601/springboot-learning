package com.example.learning.service.excel.importing;

import com.example.learning.dto.ExcelImportResult;
import com.example.learning.entity.ExcelImportRecordEntity;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class PoiExcelImportService {

    private static final int CHUNK_SIZE = 5_000;

    private final ExcelImportRowMapper rowMapper;
    private final ExcelImportPersistenceService persistenceService;

    public ExcelImportResult importFile(MultipartFile file, boolean resetTable) throws IOException {
        validateInputFile(file);

        try (InputStream inputStream = file.getInputStream(); Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            rowMapper.validatePoiHeader(sheet.getRow(0));

            List<ExcelImportRecordEntity> rows = new ArrayList<>();
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null || rowMapper.isPoiRowEmpty(row)) {
                    continue;
                }

                rows.add(rowMapper.mapRow(
                        rowIndex + 1,
                        rowMapper.getPoiCellText(row, 0),
                        rowMapper.getPoiCellText(row, 1),
                        rowMapper.getPoiCellText(row, 2),
                        rowMapper.getPoiCellText(row, 3),
                        rowMapper.getPoiCellText(row, 4),
                        rowMapper.getPoiCellText(row, 5),
                        rowMapper.getPoiCellText(row, 6),
                        rowMapper.getPoiCellText(row, 7)
                ));
            }

            persistenceService.resetTableForImport(resetTable);
            int importedRows = persistenceService.saveChunk(rows);
            return persistenceService.buildResult("poi", file, resetTable, importedRows);
        }
    }

    @Transactional
    public ExcelImportResult importFileInChunks(MultipartFile file, boolean resetTable) throws IOException {
        validateInputFile(file);
        persistenceService.resetTableForImport(resetTable);

        int importedRows = 0;
        try (InputStream inputStream = file.getInputStream(); Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            rowMapper.validatePoiHeader(sheet.getRow(0));

            List<ExcelImportRecordEntity> chunk = new ArrayList<>(CHUNK_SIZE);
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null || rowMapper.isPoiRowEmpty(row)) {
                    continue;
                }

                chunk.add(rowMapper.mapRow(
                        rowIndex + 1,
                        rowMapper.getPoiCellText(row, 0),
                        rowMapper.getPoiCellText(row, 1),
                        rowMapper.getPoiCellText(row, 2),
                        rowMapper.getPoiCellText(row, 3),
                        rowMapper.getPoiCellText(row, 4),
                        rowMapper.getPoiCellText(row, 5),
                        rowMapper.getPoiCellText(row, 6),
                        rowMapper.getPoiCellText(row, 7)
                ));

                if (chunk.size() == CHUNK_SIZE) {
                    importedRows += persistenceService.saveChunk(chunk);
                    chunk.clear();
                }
            }

            importedRows += persistenceService.saveChunk(chunk);
            return persistenceService.buildResult("poi-chunk-5000", file, resetTable, importedRows);
        }
    }

    private void validateInputFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File import khong duoc de trong.");
        }
    }
}
