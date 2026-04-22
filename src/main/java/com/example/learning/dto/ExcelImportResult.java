package com.example.learning.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ExcelImportResult {

    private String reader;
    private String fileName;
    private int importedRows;
    private boolean resetTable;
    private String message;
}
