package com.example.learning.service.excel.importing;

import com.example.learning.dto.ExcelImportResult;
import com.example.learning.entity.ExcelImportRecordEntity;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ExcelImportService {

    private static final String SHEET_NAME = "import-data";
    private static final List<Integer> SUPPORTED_SAMPLE_ROW_COUNTS = List.of(
            20_000,
            50_000,
            100_000,
            200_000,
            500_000,
            700_000,
            1_000_000
    );
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private final ExcelImportRowMapper rowMapper;
    private final PoiExcelImportService poiExcelImportService;
    private final PoiSaxExcelImportService poiSaxExcelImportService;
    private final FastExcelImportService fastExcelImportService;

    public void writeTemplate(OutputStream outputStream) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(SHEET_NAME);
            writeHeader(sheet.createRow(0));
            workbook.write(outputStream);
        }
    }

    public void writeSampleData(OutputStream outputStream, int rowCount) throws IOException {
        validateSampleRowCount(rowCount);

        try (org.dhatim.fastexcel.Workbook workbook = new org.dhatim.fastexcel.Workbook(outputStream, "learning", "1.0")) {
            org.dhatim.fastexcel.Worksheet worksheet = workbook.newWorksheet(SHEET_NAME);
            writeStreamingHeader(worksheet);

            for (int rowIndex = 1; rowIndex <= rowCount; rowIndex++) {
                ExcelImportRecordEntity sample = buildSampleEntity(rowIndex);
                writeStreamingEntityRow(worksheet, rowIndex, sample);
            }
        }
    }

    public List<Integer> getSupportedSampleRowCounts() {
        return SUPPORTED_SAMPLE_ROW_COUNTS;
    }

    public ExcelImportResult importByPoi(MultipartFile file, boolean resetTable) throws IOException {
        return poiExcelImportService.importFile(file, resetTable);
    }

    public ExcelImportResult importByPoiInChunks(MultipartFile file, boolean resetTable) throws IOException {
        return poiExcelImportService.importFileInChunks(file, resetTable);
    }

    public ExcelImportResult importByPoiSax(MultipartFile file, boolean resetTable) throws IOException {
        return poiSaxExcelImportService.importFile(file, resetTable);
    }

    public ExcelImportResult importByPoiSaxInChunks(MultipartFile file, boolean resetTable) throws IOException {
        return poiSaxExcelImportService.importFileInChunks(file, resetTable);
    }

    public ExcelImportResult importByFastExcel(MultipartFile file, boolean resetTable) throws IOException {
        return fastExcelImportService.importFile(file, resetTable);
    }

    public ExcelImportResult importByFastExcelInChunks(MultipartFile file, boolean resetTable) throws IOException {
        return fastExcelImportService.importFileInChunks(file, resetTable);
    }

    private void validateSampleRowCount(int rowCount) {
        if (!SUPPORTED_SAMPLE_ROW_COUNTS.contains(rowCount)) {
            throw new IllegalArgumentException(
                    "rowCount khong hop le. Chi ho tro cac moc: " + SUPPORTED_SAMPLE_ROW_COUNTS
            );
        }
    }

    private void writeHeader(Row row) {
        List<String> headers = rowMapper.headers();
        for (int columnIndex = 0; columnIndex < headers.size(); columnIndex++) {
            row.createCell(columnIndex).setCellValue(headers.get(columnIndex));
        }
    }

    private void writeStreamingHeader(org.dhatim.fastexcel.Worksheet worksheet) {
        List<String> headers = rowMapper.headers();
        for (int columnIndex = 0; columnIndex < headers.size(); columnIndex++) {
            worksheet.value(0, columnIndex, headers.get(columnIndex));
        }
    }

    private void writeStreamingEntityRow(
            org.dhatim.fastexcel.Worksheet worksheet,
            int rowIndex,
            ExcelImportRecordEntity entity
    ) {
        worksheet.value(rowIndex, 0, entity.getEmployeeCode());
        worksheet.value(rowIndex, 1, entity.getFullName());
        worksheet.value(rowIndex, 2, entity.getEmail());
        worksheet.value(rowIndex, 3, entity.getAge());
        worksheet.value(rowIndex, 4, entity.getDepartment());
        worksheet.value(rowIndex, 5, entity.getSalary().doubleValue());
        worksheet.value(rowIndex, 6, DATE_FORMATTER.format(entity.getJoinDate()));
        worksheet.value(rowIndex, 7, entity.getActive());
    }

    private ExcelImportRecordEntity buildSampleEntity(int rowIndex) {
        return ExcelImportRecordEntity.builder()
                .employeeCode("EMP-" + String.format("%05d", rowIndex))
                .fullName("Nhan vien " + rowIndex)
                .email("employee" + rowIndex + "@example.com")
                .age(20 + (rowIndex % 25))
                .department(switch (rowIndex % 5) {
                    case 0 -> "IT";
                    case 1 -> "HR";
                    case 2 -> "FINANCE";
                    case 3 -> "SALES";
                    default -> "OPS";
                })
                .salary(BigDecimal.valueOf(8_000_000L + (rowIndex * 1_250L)))
                .joinDate(LocalDate.of(2021, 1, 1).plusDays(rowIndex % 1_460))
                .active(rowIndex % 7 != 0)
                .build();
    }
}
