package com.example.learning.service.excel.importing;

import com.example.learning.entity.ExcelImportRecordEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.stereotype.Component;

@Component
public class ExcelImportRowMapper {

    private static final DataFormatter DATA_FORMATTER = new DataFormatter(Locale.US);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final List<String> HEADERS = List.of(
            "employeeCode",
            "fullName",
            "email",
            "age",
            "department",
            "salary",
            "joinDate",
            "active"
    );

    public List<String> headers() {
        return HEADERS;
    }

    public void validatePoiHeader(Row headerRow) {
        if (headerRow == null) {
            throw new IllegalArgumentException("File Excel khong co dong header.");
        }

        for (int columnIndex = 0; columnIndex < HEADERS.size(); columnIndex++) {
            validateHeaderCell(getPoiCellText(headerRow, columnIndex), columnIndex);
        }
    }

    public void validateFastExcelHeader(org.dhatim.fastexcel.reader.Row headerRow) {
        if (headerRow == null) {
            throw new IllegalArgumentException("File Excel khong co dong header.");
        }

        for (int columnIndex = 0; columnIndex < HEADERS.size(); columnIndex++) {
            validateHeaderCell(getFastExcelCellText(headerRow, columnIndex), columnIndex);
        }
    }

    public void validateHeaderValues(List<String> headerValues) {
        if (headerValues == null || headerValues.isEmpty()) {
            throw new IllegalArgumentException("File Excel khong co dong header.");
        }

        for (int columnIndex = 0; columnIndex < HEADERS.size(); columnIndex++) {
            validateHeaderCell(getCellText(headerValues, columnIndex), columnIndex);
        }
    }

    public String getPoiCellText(Row row, int columnIndex) {
        Cell cell = row.getCell(columnIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        return cell == null ? "" : DATA_FORMATTER.formatCellValue(cell).trim();
    }

    public boolean isPoiRowEmpty(Row row) {
        for (int columnIndex = 0; columnIndex < HEADERS.size(); columnIndex++) {
            if (!getPoiCellText(row, columnIndex).isBlank()) {
                return false;
            }
        }
        return true;
    }

    public String getFastExcelCellText(org.dhatim.fastexcel.reader.Row row, int columnIndex) {
        org.dhatim.fastexcel.reader.Cell cell = row.getCell(columnIndex);
        return cell == null ? "" : cell.getText().trim();
    }

    public boolean isFastExcelRowEmpty(org.dhatim.fastexcel.reader.Row row) {
        for (int columnIndex = 0; columnIndex < HEADERS.size(); columnIndex++) {
            if (!getFastExcelCellText(row, columnIndex).isBlank()) {
                return false;
            }
        }
        return true;
    }

    public String getCellText(List<String> rowValues, int columnIndex) {
        if (rowValues == null || columnIndex >= rowValues.size()) {
            return "";
        }
        String value = rowValues.get(columnIndex);
        return value == null ? "" : value.trim();
    }

    public boolean isRowEmpty(List<String> rowValues) {
        for (int columnIndex = 0; columnIndex < HEADERS.size(); columnIndex++) {
            if (!getCellText(rowValues, columnIndex).isBlank()) {
                return false;
            }
        }
        return true;
    }

    public ExcelImportRecordEntity mapRow(
            int displayRowNumber,
            String employeeCode,
            String fullName,
            String email,
            String age,
            String department,
            String salary,
            String joinDate,
            String active
    ) {
        requireText(employeeCode, "employeeCode", displayRowNumber);
        requireText(fullName, "fullName", displayRowNumber);
        requireText(email, "email", displayRowNumber);
        requireText(department, "department", displayRowNumber);

        return ExcelImportRecordEntity.builder()
                .employeeCode(employeeCode.trim())
                .fullName(fullName.trim())
                .email(email.trim())
                .age(parseInteger(age, "age", displayRowNumber))
                .department(department.trim())
                .salary(parseBigDecimal(salary, "salary", displayRowNumber))
                .joinDate(parseLocalDate(joinDate, "joinDate", displayRowNumber))
                .active(parseBoolean(active, "active", displayRowNumber))
                .build();
    }

    private void validateHeaderCell(String actual, int columnIndex) {
        String expected = HEADERS.get(columnIndex);
        if (!Objects.equals(expected, actual)) {
            throw new IllegalArgumentException(
                    "Header khong hop le tai cot " + (columnIndex + 1) + ". Expected=" + expected + ", actual=" + actual
            );
        }
    }

    private Integer parseInteger(String value, String field, int rowNumber) {
        requireText(value, field, rowNumber);
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Gia tri khong hop le cho " + field + " tai dong " + rowNumber + ": " + value);
        }
    }

    private BigDecimal parseBigDecimal(String value, String field, int rowNumber) {
        requireText(value, field, rowNumber);
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Gia tri khong hop le cho " + field + " tai dong " + rowNumber + ": " + value);
        }
    }

    private LocalDate parseLocalDate(String value, String field, int rowNumber) {
        requireText(value, field, rowNumber);
        try {
            return LocalDate.parse(value.trim(), DATE_FORMATTER);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Ngay khong hop le cho " + field + " tai dong " + rowNumber + ": " + value);
        }
    }

    private Boolean parseBoolean(String value, String field, int rowNumber) {
        requireText(value, field, rowNumber);
        if ("true".equalsIgnoreCase(value.trim())) {
            return Boolean.TRUE;
        }
        if ("false".equalsIgnoreCase(value.trim())) {
            return Boolean.FALSE;
        }
        throw new IllegalArgumentException("Boolean khong hop le cho " + field + " tai dong " + rowNumber + ": " + value);
    }

    private void requireText(String value, String field, int rowNumber) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Truong " + field + " khong duoc de trong tai dong " + rowNumber);
        }
    }
}
