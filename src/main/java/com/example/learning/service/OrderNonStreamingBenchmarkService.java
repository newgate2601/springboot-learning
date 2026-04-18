package com.example.learning.service;

import com.example.learning.dto.OrderExportRow;
import com.example.learning.repository.OrderRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.locks.LockSupport;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@AllArgsConstructor
public class OrderNonStreamingBenchmarkService {

    private static final int EXPORT_LIMIT = 500_000;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final long GC_SETTLE_NANOS = 200_000_000L;

    private final OrderRepository orderRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public byte[] exportOrdersByPoi() throws IOException {
        List<OrderExportRow> rows = loadRowsForExport();
        BenchmarkResult result = runWriteBenchmark("poi", rows, this::writeOrdersWithPoi);
        logWriteBenchmark(result);
        return result.fileContent();
    }

    @Transactional(readOnly = true)
    public byte[] exportOrdersByFastExcel() throws IOException {
        List<OrderExportRow> rows = loadRowsForExport();
        BenchmarkResult result = runWriteBenchmark("fastexcel", rows, this::writeOrdersWithFastExcel);
        logWriteBenchmark(result);
        return result.fileContent();
    }

    private List<OrderExportRow> loadRowsForExport() {
        stabilizeHeap();
        MemorySnapshot beforeLoadSnapshot = captureMemorySnapshot();
        long loadStart = System.nanoTime();

        List<OrderExportRow> rows = orderRepository.findOrderExportRows(
                PageRequest.of(0, EXPORT_LIMIT, Sort.by(Sort.Direction.ASC, "id"))
        );

        entityManager.clear();

        long afterLoad = System.nanoTime();
        MemorySnapshot afterLoadSnapshot = captureMemorySnapshot();

        stabilizeHeap();
        MemorySnapshot afterLoadForcedGcSnapshot = captureMemorySnapshot();

        log.info(
                "order-non-streaming load rows={} loadMs={} heapBeforeLoadMb={} heapAfterLoadMb={} heapAfterLoadForcedGcMb={} heapLoadTransientMb={} heapLoadRetainedMb={} gcCountDuringLoad={} gcTimeMsDuringLoad={}",
                rows.size(),
                nanosToMillis(afterLoad - loadStart),
                round(bytesToMb(beforeLoadSnapshot.heapUsedBytes())),
                round(bytesToMb(afterLoadSnapshot.heapUsedBytes())),
                round(bytesToMb(afterLoadForcedGcSnapshot.heapUsedBytes())),
                round(bytesToMb(afterLoadSnapshot.heapUsedBytes() - beforeLoadSnapshot.heapUsedBytes())),
                round(bytesToMb(afterLoadForcedGcSnapshot.heapUsedBytes() - beforeLoadSnapshot.heapUsedBytes())),
                afterLoadSnapshot.gcCount() - beforeLoadSnapshot.gcCount(),
                afterLoadSnapshot.gcTimeMs() - beforeLoadSnapshot.gcTimeMs()
        );

        return rows;
    }

    private BenchmarkResult runWriteBenchmark(
            String library,
            List<OrderExportRow> rows,
            ExcelWriter excelWriter
    ) throws IOException {
        stabilizeHeap();
        MemorySnapshot beforeWriteSnapshot = captureMemorySnapshot();
        long writeStart = System.nanoTime();

        byte[] fileContent = excelWriter.write(rows);

        long afterWrite = System.nanoTime();
        MemorySnapshot afterWriteSnapshot = captureMemorySnapshot();

        stabilizeHeap();
        MemorySnapshot afterWriteForcedGcSnapshot = captureMemorySnapshot();

        return new BenchmarkResult(
                library,
                rows.size(),
                fileContent,
                writeStart,
                afterWrite,
                beforeWriteSnapshot,
                afterWriteSnapshot,
                afterWriteForcedGcSnapshot
        );
    }

    private byte[] writeOrdersWithPoi(List<OrderExportRow> rows) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            XSSFSheet sheet = workbook.createSheet("orders");
            writeHeader(sheet.createRow(0));

            int rowIndex = 1;
            for (OrderExportRow rowData : rows) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(nullSafeLong(rowData.id()));
                row.createCell(1).setCellValue(nullSafeString(rowData.orderNo()));
                row.createCell(2).setCellValue(nullSafeLong(rowData.customerId()));
                row.createCell(3).setCellValue(nullSafeString(rowData.status()));
                row.createCell(4).setCellValue(rowData.totalAmount() == null ? 0D : rowData.totalAmount().doubleValue());
                row.createCell(5).setCellValue(formatDateTime(rowData.orderDate()));
                row.createCell(6).setCellValue(formatDateTime(rowData.createdAt()));
                row.createCell(7).setCellValue(formatDateTime(rowData.updatedAt()));
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private byte[] writeOrdersWithFastExcel(List<OrderExportRow> rows) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             Workbook workbook = new Workbook(outputStream, "learning", "1.0")) {

            Worksheet sheet = workbook.newWorksheet("orders");
            writeHeader(sheet);

            int rowIndex = 1;
            for (OrderExportRow rowData : rows) {
                sheet.value(rowIndex, 0, rowData.id());
                sheet.value(rowIndex, 1, rowData.orderNo());
                sheet.value(rowIndex, 2, rowData.customerId());
                sheet.value(rowIndex, 3, rowData.status());
                sheet.value(rowIndex, 4, rowData.totalAmount() == null ? null : rowData.totalAmount().doubleValue());
                sheet.value(rowIndex, 5, formatDateTime(rowData.orderDate()));
                sheet.value(rowIndex, 6, formatDateTime(rowData.createdAt()));
                sheet.value(rowIndex, 7, formatDateTime(rowData.updatedAt()));
                rowIndex++;
            }

            workbook.finish();
            return outputStream.toByteArray();
        }
    }

    private void writeHeader(Row header) {
        header.createCell(0).setCellValue("id");
        header.createCell(1).setCellValue("orderNo");
        header.createCell(2).setCellValue("customerId");
        header.createCell(3).setCellValue("status");
        header.createCell(4).setCellValue("totalAmount");
        header.createCell(5).setCellValue("orderDate");
        header.createCell(6).setCellValue("createdAt");
        header.createCell(7).setCellValue("updatedAt");
    }

    private void writeHeader(Worksheet header) throws IOException {
        header.value(0, 0, "id");
        header.value(0, 1, "orderNo");
        header.value(0, 2, "customerId");
        header.value(0, 3, "status");
        header.value(0, 4, "totalAmount");
        header.value(0, 5, "orderDate");
        header.value(0, 6, "createdAt");
        header.value(0, 7, "updatedAt");
    }

    private void logWriteBenchmark(BenchmarkResult result) {
        long writeMs = nanosToMillis(result.afterWriteNanos() - result.writeStartNanos());
        double fileSizeKb = result.fileContent().length / 1024.0;

        log.info(
                "order-non-streaming benchmark library={} rows={} writeMs={} fileSizeKb={} heapBeforeWriteMb={} heapAfterWriteMb={} heapAfterWriteForcedGcMb={} heapWriteTransientMb={} heapWriteRetainedMb={} nonHeapBeforeWriteMb={} nonHeapAfterWriteMb={} gcCountDuringWrite={} gcTimeMsDuringWrite={}",
                result.library(),
                result.rowCount(),
                writeMs,
                round(fileSizeKb),
                round(bytesToMb(result.beforeWriteSnapshot().heapUsedBytes())),
                round(bytesToMb(result.afterWriteSnapshot().heapUsedBytes())),
                round(bytesToMb(result.afterWriteForcedGcSnapshot().heapUsedBytes())),
                round(bytesToMb(result.afterWriteSnapshot().heapUsedBytes() - result.beforeWriteSnapshot().heapUsedBytes())),
                round(bytesToMb(result.afterWriteForcedGcSnapshot().heapUsedBytes() - result.beforeWriteSnapshot().heapUsedBytes())),
                round(bytesToMb(result.beforeWriteSnapshot().nonHeapUsedBytes())),
                round(bytesToMb(result.afterWriteSnapshot().nonHeapUsedBytes())),
                result.afterWriteSnapshot().gcCount() - result.beforeWriteSnapshot().gcCount(),
                result.afterWriteSnapshot().gcTimeMs() - result.beforeWriteSnapshot().gcTimeMs()
        );
    }

    private String formatDateTime(OffsetDateTime dateTime) {
        return dateTime == null ? "" : DATE_TIME_FORMATTER.format(dateTime);
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private long nanosToMillis(long nanos) {
        return nanos / 1_000_000;
    }

    private void stabilizeHeap() {
        System.gc();
        System.runFinalization();
        LockSupport.parkNanos(GC_SETTLE_NANOS);
        System.gc();
        LockSupport.parkNanos(GC_SETTLE_NANOS);
    }

    private MemorySnapshot captureMemorySnapshot() {
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = memoryMXBean.getNonHeapMemoryUsage();

        long gcCount = 0L;
        long gcTimeMs = 0L;
        for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) {
            long collectionCount = gcBean.getCollectionCount();
            long collectionTime = gcBean.getCollectionTime();
            if (collectionCount > 0) {
                gcCount += collectionCount;
            }
            if (collectionTime > 0) {
                gcTimeMs += collectionTime;
            }
        }

        return new MemorySnapshot(heapUsage.getUsed(), nonHeapUsage.getUsed(), gcCount, gcTimeMs);
    }

    private double bytesToMb(long bytes) {
        return bytes / 1024.0 / 1024.0;
    }

    private double nullSafeLong(Long value) {
        return value == null ? 0D : value.doubleValue();
    }

    private String nullSafeString(String value) {
        return value == null ? "" : value;
    }

    @FunctionalInterface
    private interface ExcelWriter {
        byte[] write(List<OrderExportRow> rows) throws IOException;
    }

    private record BenchmarkResult(
            String library,
            int rowCount,
            byte[] fileContent,
            long writeStartNanos,
            long afterWriteNanos,
            MemorySnapshot beforeWriteSnapshot,
            MemorySnapshot afterWriteSnapshot,
            MemorySnapshot afterWriteForcedGcSnapshot
    ) {
    }

    private record MemorySnapshot(
            long heapUsedBytes,
            long nonHeapUsedBytes,
            long gcCount,
            long gcTimeMs
    ) {
    }
}
