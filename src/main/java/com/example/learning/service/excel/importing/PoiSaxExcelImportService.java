package com.example.learning.service.excel.importing;

import com.example.learning.dto.ExcelImportResult;
import com.example.learning.entity.ExcelImportRecordEntity;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.util.XMLHelper;
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

@Service
@RequiredArgsConstructor
public class PoiSaxExcelImportService {

    private static final int CHUNK_SIZE = 5_000;

    private final ExcelImportRowMapper rowMapper;
    private final ExcelImportPersistenceService persistenceService;

    public ExcelImportResult importFile(MultipartFile file, boolean resetTable) throws IOException {
        validateInputFile(file);

        SaxImportAccumulator accumulator = new SaxImportAccumulator();
        parseWorkbook(file, accumulator);

        persistenceService.resetTableForImport(resetTable);
        int importedRows = persistenceService.saveChunk(accumulator.rows());
        return persistenceService.buildResult("poi-sax", file, resetTable, importedRows);
    }

    @Transactional
    public ExcelImportResult importFileInChunks(MultipartFile file, boolean resetTable) throws IOException {
        validateInputFile(file);
        persistenceService.resetTableForImport(resetTable);

        ChunkedSaxImportAccumulator accumulator = new ChunkedSaxImportAccumulator();
        parseWorkbook(file, accumulator);

        int importedRows = accumulator.flushRemaining();
        return persistenceService.buildResult("poi-sax-chunk-5000", file, resetTable, importedRows);
    }

    @SneakyThrows
    private void parseWorkbook(MultipartFile file, SaxRowConsumer rowConsumer) throws IOException {
        try (
                InputStream inputStream = file.getInputStream();
                OPCPackage opcPackage = OPCPackage.open(inputStream)
        ) {
            ReadOnlySharedStringsTable sharedStrings = new ReadOnlySharedStringsTable(opcPackage);
            XSSFReader reader = new XSSFReader(opcPackage);
            StylesTable styles = reader.getStylesTable();
            XSSFReader.SheetIterator sheetIterator = (XSSFReader.SheetIterator) reader.getSheetsData();
            if (!sheetIterator.hasNext()) {
                throw new IllegalArgumentException("File Excel khong co sheet nao.");
            }

            try (InputStream sheetStream = sheetIterator.next()) {
                XMLReader parser = XMLHelper.newXMLReader();
                DataFormatter dataFormatter = new DataFormatter();
                SaxSheetHandler sheetHandler = new SaxSheetHandler(rowConsumer);
                XSSFSheetXMLHandler contentHandler = new XSSFSheetXMLHandler(
                        styles,
                        null,
                        sharedStrings,
                        sheetHandler,
                        dataFormatter,
                        false
                );
                parser.setContentHandler(contentHandler);
                parser.parse(new InputSource(sheetStream));
                rowConsumer.finish();
            }
        } catch (OpenXML4JException | SAXException exception) {
            throw new IOException("Khong the doc file Excel bang POI SAX.", exception);
        }
    }

    private void validateInputFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File import khong duoc de trong.");
        }
    }

    private interface SaxRowConsumer {
        void accept(int rowNumber, List<String> rowValues);

        default void finish() {
        }
    }

    private final class SaxImportAccumulator implements SaxRowConsumer {

        private final List<ExcelImportRecordEntity> rows = new ArrayList<>();
        private boolean headerProcessed;

        @Override
        public void accept(int rowNumber, List<String> rowValues) {
            if (!headerProcessed) {
                rowMapper.validateHeaderValues(rowValues);
                headerProcessed = true;
                return;
            }

            if (rowMapper.isRowEmpty(rowValues)) {
                return;
            }

            rows.add(mapRow(rowNumber, rowValues));
        }

        @Override
        public void finish() {
            if (!headerProcessed) {
                throw new IllegalArgumentException("File Excel khong co dong header.");
            }
        }

        public List<ExcelImportRecordEntity> rows() {
            return rows;
        }
    }

    private final class ChunkedSaxImportAccumulator implements SaxRowConsumer {

        private final List<ExcelImportRecordEntity> chunk = new ArrayList<>(CHUNK_SIZE);
        private int importedRows;
        private boolean headerProcessed;

        @Override
        public void accept(int rowNumber, List<String> rowValues) {
            if (!headerProcessed) {
                rowMapper.validateHeaderValues(rowValues);
                headerProcessed = true;
                return;
            }

            if (rowMapper.isRowEmpty(rowValues)) {
                return;
            }

            chunk.add(mapRow(rowNumber, rowValues));
            if (chunk.size() == CHUNK_SIZE) {
                importedRows += persistenceService.saveChunk(chunk);
                chunk.clear();
            }
        }

        @Override
        public void finish() {
            if (!headerProcessed) {
                throw new IllegalArgumentException("File Excel khong co dong header.");
            }
        }

        public int flushRemaining() {
            importedRows += persistenceService.saveChunk(chunk);
            chunk.clear();
            return importedRows;
        }
    }

    private ExcelImportRecordEntity mapRow(int rowNumber, List<String> rowValues) {
        return rowMapper.mapRow(
                rowNumber,
                rowMapper.getCellText(rowValues, 0),
                rowMapper.getCellText(rowValues, 1),
                rowMapper.getCellText(rowValues, 2),
                rowMapper.getCellText(rowValues, 3),
                rowMapper.getCellText(rowValues, 4),
                rowMapper.getCellText(rowValues, 5),
                rowMapper.getCellText(rowValues, 6),
                rowMapper.getCellText(rowValues, 7)
        );
    }

    private static final class SaxSheetHandler implements XSSFSheetXMLHandler.SheetContentsHandler {

        private final SaxRowConsumer rowConsumer;
        private List<String> currentRowValues;
        private int currentRowNumber;

        private SaxSheetHandler(SaxRowConsumer rowConsumer) {
            this.rowConsumer = rowConsumer;
        }

        @Override
        public void startRow(int rowNum) {
            currentRowNumber = rowNum + 1;
            currentRowValues = new ArrayList<>();
        }

        @Override
        public void endRow(int rowNum) {
            rowConsumer.accept(currentRowNumber, currentRowValues);
        }

        @Override
        public void cell(String cellReference, String formattedValue, XSSFComment comment) {
            if (cellReference == null) {
                return;
            }

            int columnIndex = new CellReference(cellReference).getCol();
            while (currentRowValues.size() <= columnIndex) {
                currentRowValues.add("");
            }
            currentRowValues.set(columnIndex, formattedValue == null ? "" : formattedValue.trim());
        }

        @Override
        public void headerFooter(String text, boolean isHeader, String tagName) {
        }
    }
}
