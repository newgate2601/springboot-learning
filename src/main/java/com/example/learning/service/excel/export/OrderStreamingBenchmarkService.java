package com.example.learning.service.excel.export;

import com.example.learning.dto.OrderExportRow;
import com.example.learning.repository.OrderStreamingExportRepository;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@AllArgsConstructor
public class OrderStreamingBenchmarkService {

    private static final int BATCH_SIZE = 50_000;
    private static final int POI_ROW_WINDOW_SIZE = 50_000;
    private static final long GC_SETTLE_NANOS = 200_000_000L;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final OrderStreamingExportRepository orderStreamingExportRepository;

    public void streamOrdersByPoi(OutputStream outputStream) throws IOException {
        streamOrders("poi-sxssf", outputStream, this::writeOrdersWithPoi);
    }

    public void streamOrdersByFastExcel(OutputStream outputStream) throws IOException {
        streamOrders("fastexcel-streaming", outputStream, this::writeOrdersWithFastExcel);
    }

    private void streamOrders(
            String library,
            OutputStream outputStream,
            StreamingExcelWriter writer
    ) throws IOException {
        stabilizeHeap();
        MemorySnapshot beforeStreamingSnapshot = captureMemorySnapshot();
        long benchmarkStart = System.nanoTime();
        long expectedRows = orderStreamingExportRepository.countOrders();

        CountingOutputStream countingOutputStream = new CountingOutputStream(outputStream);
        BenchmarkAccumulator accumulator = new BenchmarkAccumulator(library, expectedRows);

        try {
            writer.write(countingOutputStream, accumulator);
        } finally {
            countingOutputStream.flush();
        }

        long benchmarkEnd = System.nanoTime();
        MemorySnapshot afterStreamingSnapshot = captureMemorySnapshot();

        stabilizeHeap();
        MemorySnapshot afterStreamingForcedGcSnapshot = captureMemorySnapshot();

        log.info(
                "order-streaming benchmark library={} expectedRows={} streamedRows={} batches={} batchSize={} totalMs={} loadMs={} writeMs={} fileSizeKb={} heapBeforeMb={} heapAfterMb={} heapAfterForcedGcMb={} heapTransientMb={} heapRetainedMb={} nonHeapBeforeMb={} nonHeapAfterMb={} gcCountDuringStreaming={} gcTimeMsDuringStreaming={}",
                accumulator.library(),
                accumulator.expectedRows(),
                accumulator.streamedRows(),
                accumulator.batchCount(),
                BATCH_SIZE,
                nanosToMillis(benchmarkEnd - benchmarkStart),
                nanosToMillis(accumulator.totalLoadNanos()),
                nanosToMillis(accumulator.totalWriteNanos()),
                round(countingOutputStream.getByteCount() / 1024.0),
                round(bytesToMb(beforeStreamingSnapshot.heapUsedBytes())),
                round(bytesToMb(afterStreamingSnapshot.heapUsedBytes())),
                round(bytesToMb(afterStreamingForcedGcSnapshot.heapUsedBytes())),
                round(bytesToMb(afterStreamingSnapshot.heapUsedBytes() - beforeStreamingSnapshot.heapUsedBytes())),
                round(bytesToMb(afterStreamingForcedGcSnapshot.heapUsedBytes() - beforeStreamingSnapshot.heapUsedBytes())),
                round(bytesToMb(beforeStreamingSnapshot.nonHeapUsedBytes())),
                round(bytesToMb(afterStreamingSnapshot.nonHeapUsedBytes())),
                afterStreamingSnapshot.gcCount() - beforeStreamingSnapshot.gcCount(),
                afterStreamingSnapshot.gcTimeMs() - beforeStreamingSnapshot.gcTimeMs()
        );
    }

    private void writeOrdersWithPoi(OutputStream outputStream, BenchmarkAccumulator accumulator) throws IOException {
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(POI_ROW_WINDOW_SIZE)) {
            workbook.setCompressTempFiles(true);

            SXSSFSheet sheet = workbook.createSheet("orders");
            writeHeader(sheet.createRow(0));

            int rowIndex = 1;
            Long lastId = null;

            while (true) {
                BatchLoadResult batch = loadNextBatch(lastId);
                accumulator.recordBatchLoad(batch);
                if (batch.rows().isEmpty()) {
                    break;
                }

                long writeStart = System.nanoTime();
                for (OrderExportRow rowData : batch.rows()) {
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
                sheet.flushRows(POI_ROW_WINDOW_SIZE);
                accumulator.recordWrite(batch.rows().size(), System.nanoTime() - writeStart);

                lastId = batch.lastId();
            }

            long finalWriteStart = System.nanoTime();
            workbook.write(outputStream);
            outputStream.flush();
            accumulator.recordFinalizeWrite(System.nanoTime() - finalWriteStart);
            workbook.dispose();
        }
    }

    private void writeOrdersWithFastExcel(OutputStream outputStream, BenchmarkAccumulator accumulator) throws IOException {
        try (Workbook workbook = new Workbook(outputStream, "learning", "1.0")) {
            Worksheet sheet = workbook.newWorksheet("orders");
            writeHeader(sheet);

            int rowIndex = 1;
            Long lastId = null;

            while (true) {
                BatchLoadResult batch = loadNextBatch(lastId);
                accumulator.recordBatchLoad(batch);
                if (batch.rows().isEmpty()) {
                    break;
                }

                long writeStart = System.nanoTime();
                for (OrderExportRow rowData : batch.rows()) {
                    sheet.value(rowIndex, 0, rowData.id());
                    writeInlineString(sheet, rowIndex, 1, rowData.orderNo());
                    sheet.value(rowIndex, 2, rowData.customerId());
                    sheet.value(rowIndex, 3, rowData.status());
                    sheet.value(rowIndex, 4, rowData.totalAmount() == null ? null : rowData.totalAmount().doubleValue());
                    writeInlineString(sheet, rowIndex, 5, formatDateTime(rowData.orderDate()));
                    writeInlineString(sheet, rowIndex, 6, formatDateTime(rowData.createdAt()));
                    writeInlineString(sheet, rowIndex, 7, formatDateTime(rowData.updatedAt()));
                    // fastexcel nhận cell data, build XML nội bộ cho sheet. Nếu XML này đủ lớn hoặc đủ nhiều row,
                    // nó sẽ đẩy XML này ra stream của workbook, thay vì giữ lại trong buffer nội bộ của sheet.
                    rowIndex++;
                }
                // Mục đích là yêu cầu thư viện đẩy phần XML của worksheet đang buffer ra Workbook/ZIP stream sớm hơn, thay vì giữ lại trong buffer nội bộ của fastexcel.
                // Tác động chính:
                //  + Giảm buffer nội bộ của chính thư viện Excel
                //  + Có thể làm dữ liệu sheet được ghi ra stream sớm hơn
                //  + Không giải phóng List<OrderExportRow> hay object do JPA/map tạo ra
                sheet.flush();
                // Mục đích là đẩy bytes đang nằm trong buffer của stream xuống tầng dưới ngay lập tức. Với case này
                // thì những bytes đã được ghi xuống outputStream sẽ được đẩy tiếp xuống response/socket.
                // Ví dụ tầng dưới có thể là:
                //  + servlet response buffer
                //  + socket TCP
                //  + buffered stream
                //  + file stream
                // Tác động chính:
                //  + giúp client nhận dữ liệu sớm hơn
                //  + giảm buffer ở tầng I/O nếu có buffer
                //  + không làm fastexcel tạo thêm XML
                //  + không giải phóng object trong heap của business code
                outputStream.flush();
                accumulator.recordWrite(batch.rows().size(), System.nanoTime() - writeStart);

                lastId = batch.lastId();
            }

            long finalWriteStart = System.nanoTime();
            workbook.finish();
            outputStream.flush();
            accumulator.recordFinalizeWrite(System.nanoTime() - finalWriteStart);
        }
    }

    private BatchLoadResult loadNextBatch(Long lastIdExclusive) {
        MemorySnapshot beforeLoadSnapshot = captureMemorySnapshot();
        long loadStart = System.nanoTime();

        List<OrderExportRow> rows = orderStreamingExportRepository.findNextBatch(lastIdExclusive, BATCH_SIZE);

        long loadNanos = System.nanoTime() - loadStart;
        MemorySnapshot afterLoadSnapshot = captureMemorySnapshot();

        Long lastLoadedId = rows.isEmpty() ? lastIdExclusive : rows.get(rows.size() - 1).id();

        log.info(
                "order-streaming load batchStartAfterId={} rows={} loadMs={} heapBeforeLoadMb={} heapAfterLoadMb={} heapLoadDeltaMb={} gcCountDuringLoad={} gcTimeMsDuringLoad={}",
                lastIdExclusive,
                rows.size(),
                nanosToMillis(loadNanos),
                round(bytesToMb(beforeLoadSnapshot.heapUsedBytes())),
                round(bytesToMb(afterLoadSnapshot.heapUsedBytes())),
                round(bytesToMb(afterLoadSnapshot.heapUsedBytes() - beforeLoadSnapshot.heapUsedBytes())),
                afterLoadSnapshot.gcCount() - beforeLoadSnapshot.gcCount(),
                afterLoadSnapshot.gcTimeMs() - beforeLoadSnapshot.gcTimeMs()
        );

        return new BatchLoadResult(rows, lastLoadedId, loadNanos);
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
        writeInlineString(header, 0, 1, "orderNo");
        header.value(0, 2, "customerId");
        header.value(0, 3, "status");
        header.value(0, 4, "totalAmount");
        writeInlineString(header, 0, 5, "orderDate");
        writeInlineString(header, 0, 6, "createdAt");
        writeInlineString(header, 0, 7, "updatedAt");
    }

    private void writeInlineString(Worksheet sheet, int rowIndex, int columnIndex, String value) throws IOException {
        sheet.inlineString(rowIndex, columnIndex, nullSafeString(value));
    }

    private String formatDateTime(OffsetDateTime dateTime) {
        return dateTime == null ? "" : DATE_TIME_FORMATTER.format(dateTime);
    }

    private double nullSafeLong(Long value) {
        return value == null ? 0D : value.doubleValue();
    }

    private String nullSafeString(String value) {
        return value == null ? "" : value;
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

    private long nanosToMillis(long nanos) {
        return nanos / 1_000_000;
    }

    private double bytesToMb(long bytes) {
        return bytes / 1024.0 / 1024.0;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    @FunctionalInterface
    private interface StreamingExcelWriter {
        void write(OutputStream outputStream, BenchmarkAccumulator accumulator) throws IOException;
    }

    private static final class CountingOutputStream extends FilterOutputStream {

        private long byteCount;

        private CountingOutputStream(OutputStream out) {
            super(out);
        }

        @Override
        public void write(int b) throws IOException {
            out.write(b);
            byteCount++;
        }

        @Override
        public void write(byte[] b) throws IOException {
            out.write(b);
            byteCount += b.length;
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            out.write(b, off, len);
            byteCount += len;
        }

        public long getByteCount() {
            return byteCount;
        }
    }

    private static final class BenchmarkAccumulator {

        private final String library;
        private final long expectedRows;
        private long streamedRows;
        private int batchCount;
        private long totalLoadNanos;
        private long totalWriteNanos;

        private BenchmarkAccumulator(String library, long expectedRows) {
            this.library = library;
            this.expectedRows = expectedRows;
        }

        private void recordBatchLoad(BatchLoadResult batch) {
            if (batch.rows().isEmpty()) {
                return;
            }
            batchCount++;
            totalLoadNanos += batch.loadNanos();
        }

        private void recordWrite(int rows, long writeNanos) {
            streamedRows += rows;
            totalWriteNanos += writeNanos;
        }

        private void recordFinalizeWrite(long writeNanos) {
            totalWriteNanos += writeNanos;
        }

        public String library() {
            return library;
        }

        public long expectedRows() {
            return expectedRows;
        }

        public long streamedRows() {
            return streamedRows;
        }

        public int batchCount() {
            return batchCount;
        }

        public long totalLoadNanos() {
            return totalLoadNanos;
        }

        public long totalWriteNanos() {
            return totalWriteNanos;
        }
    }

    private record BatchLoadResult(
            List<OrderExportRow> rows,
            Long lastId,
            long loadNanos
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
