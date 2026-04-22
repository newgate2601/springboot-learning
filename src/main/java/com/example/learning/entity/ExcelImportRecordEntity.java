package com.example.learning.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "excel_import_records")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ExcelImportRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_code", nullable = false, length = 32)
    private String employeeCode;

    @Column(name = "full_name", nullable = false, length = 128)
    private String fullName;

    @Column(name = "email", nullable = false, length = 180)
    private String email;

    @Column(name = "age", nullable = false)
    private Integer age;

    @Column(name = "department", nullable = false, length = 64)
    private String department;

    @Column(name = "salary", nullable = false, precision = 18, scale = 2)
    private BigDecimal salary;

    @Column(name = "join_date", nullable = false)
    private LocalDate joinDate;

    @Column(name = "active", nullable = false)
    private Boolean active;
}
