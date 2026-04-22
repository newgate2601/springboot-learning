package com.example.learning.service.excel.importing;

import com.example.learning.dto.ExcelImportResult;
import com.example.learning.entity.ExcelImportRecordEntity;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.Row;
import org.dhatim.fastexcel.reader.Sheet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class FastExcelImportService {

    private static final int CHUNK_SIZE = 5_000;

    private final ExcelImportRowMapper rowMapper;
    private final ExcelImportPersistenceService persistenceService;

    public ExcelImportResult importFile(MultipartFile file, boolean resetTable) throws IOException {
        validateInputFile(file);

        try (InputStream inputStream = file.getInputStream(); ReadableWorkbook workbook = new ReadableWorkbook(inputStream)) {
            Sheet sheet = workbook.getFirstSheet();
            if (sheet == null) {
                throw new IllegalArgumentException("File Excel khong co sheet nao.");
            }

            try (Stream<Row> stream = sheet.openStream()) {
                Iterator<Row> iterator = stream.iterator();
                if (!iterator.hasNext()) {
                    throw new IllegalArgumentException("File Excel khong co header.");
                }

                Row headerRow = iterator.next();
                rowMapper.validateFastExcelHeader(headerRow);

                List<ExcelImportRecordEntity> rows = new ArrayList<>();
                int displayRowNumber = 2;
                while (iterator.hasNext()) {
                    Row row = iterator.next();
                    if (rowMapper.isFastExcelRowEmpty(row)) {
                        displayRowNumber++;
                        continue;
                    }

                    rows.add(rowMapper.mapRow(
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
                    displayRowNumber++;
                }

                persistenceService.resetTableForImport(resetTable);
                int importedRows = persistenceService.saveChunk(rows);
                return persistenceService.buildResult("fastexcel", file, resetTable, importedRows);
            }
        }
    }

    @Transactional
    public ExcelImportResult importFileInChunks(MultipartFile file, boolean resetTable) throws IOException {
        validateInputFile(file);
        persistenceService.resetTableForImport(resetTable);

        int importedRows = 0;
        try (InputStream inputStream = file.getInputStream(); ReadableWorkbook workbook = new ReadableWorkbook(inputStream)) {
            Sheet sheet = workbook.getFirstSheet();
            if (sheet == null) {
                throw new IllegalArgumentException("File Excel khong co sheet nao.");
            }

            try (Stream<Row> stream = sheet.openStream()) {
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

                    if (chunk.size() == CHUNK_SIZE) {
                        importedRows += persistenceService.saveChunk(chunk);
                        chunk.clear();
                    }
                    displayRowNumber++;
                }

                importedRows += persistenceService.saveChunk(chunk);
                return persistenceService.buildResult("fastexcel-chunk-5000", file, resetTable, importedRows);
            }
        }
    }

    private void validateInputFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File import khong duoc de trong.");
        }
    }
}
