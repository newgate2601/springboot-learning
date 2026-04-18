package com.example.learning.service;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Cảm giác méo khác biệt mấy :))
@Service
@Slf4j
@RequiredArgsConstructor
public class OrderFastExcelJdbcBenchmarkService {

    private static final int BATCH_SIZE = 50_000;
    private static final int FETCH_SIZE = 50_000;
    private static final int SHEET_FLUSH_INTERVAL = 50_000;
    private static final long GC_SETTLE_NANOS = 200_000_000L;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private static final String COUNT_SQL = """
            select count(o.id)
            from orders o
            """;

    private static final String CURSOR_SQL = """
            select
                o.id,
                o.order_no,
                o.customer_id,
                o.status,
                o.total_amount,
                o.order_date,
                o.created_at,
                o.updated_at
            from orders o
            """;

    private static final String KEYSET_BATCH_SQL = """
            select
                o.id,
                o.order_no,
                o.customer_id,
                o.status,
                o.total_amount,
                o.order_date,
                o.created_at,
                o.updated_at
            from orders o
            where o.id > ?
            limit ?
            """;

    private final JdbcTemplate jdbcTemplate;

    @Transactional(readOnly = true)
    public void exportByCursor(OutputStream outputStream) throws IOException {
        export("fastexcel-jdbc-cursor", outputStream, this::writeByCursor);
    }

    @Transactional(readOnly = true)
    public void exportByKeysetBatch(OutputStream outputStream) throws IOException {
        export("fastexcel-jdbc-keyset-batch", outputStream, this::writeByKeysetBatch);
    }

    private void export(String benchmarkName, OutputStream outputStream, BenchmarkWriter benchmarkWriter) throws IOException {
        stabilizeHeap();
        MemorySnapshot beforeStreamingSnapshot = captureMemorySnapshot();
        long benchmarkStart = System.nanoTime();
        long expectedRows = countOrders();

        CountingOutputStream countingOutputStream = new CountingOutputStream(outputStream);
        BenchmarkAccumulator accumulator = new BenchmarkAccumulator(benchmarkName, expectedRows);

        try (Workbook workbook = new Workbook(countingOutputStream, "learning", "1.0")) {
            Worksheet sheet = workbook.newWorksheet("orders");
            writeHeader(sheet);
            benchmarkWriter.write(sheet, accumulator);
            workbook.finish();
            countingOutputStream.flush();
        }

        long benchmarkEnd = System.nanoTime();
        MemorySnapshot afterStreamingSnapshot = captureMemorySnapshot();

        stabilizeHeap();
        MemorySnapshot afterForcedGcSnapshot = captureMemorySnapshot();

        log.info(
                "order-fastexcel-jdbc benchmark mode={} expectedRows={} streamedRows={} queries={} batchSize={} fetchSize={} totalMs={} queryMs={} writeMs={} fileSizeKb={} heapBeforeMb={} heapAfterMb={} heapAfterForcedGcMb={} heapTransientMb={} heapRetainedMb={} nonHeapBeforeMb={} nonHeapAfterMb={} gcCountDuringStreaming={} gcTimeMsDuringStreaming={}",
                accumulator.benchmarkName(),
                accumulator.expectedRows(),
                accumulator.streamedRows(),
                accumulator.queryCount(),
                BATCH_SIZE,
                FETCH_SIZE,
                nanosToMillis(benchmarkEnd - benchmarkStart),
                nanosToMillis(accumulator.totalQueryNanos()),
                nanosToMillis(accumulator.totalWriteNanos()),
                round(countingOutputStream.getByteCount() / 1024.0),
                round(bytesToMb(beforeStreamingSnapshot.heapUsedBytes())),
                round(bytesToMb(afterStreamingSnapshot.heapUsedBytes())),
                round(bytesToMb(afterForcedGcSnapshot.heapUsedBytes())),
                round(bytesToMb(afterStreamingSnapshot.heapUsedBytes() - beforeStreamingSnapshot.heapUsedBytes())),
                round(bytesToMb(afterForcedGcSnapshot.heapUsedBytes() - beforeStreamingSnapshot.heapUsedBytes())),
                round(bytesToMb(beforeStreamingSnapshot.nonHeapUsedBytes())),
                round(bytesToMb(afterStreamingSnapshot.nonHeapUsedBytes())),
                afterStreamingSnapshot.gcCount() - beforeStreamingSnapshot.gcCount(),
                afterStreamingSnapshot.gcTimeMs() - beforeStreamingSnapshot.gcTimeMs()
        );
    }

    private void writeByCursor(Worksheet sheet, BenchmarkAccumulator accumulator) throws IOException {
        AtomicLong rowIndex = new AtomicLong(1);
        MemorySnapshot beforeQuerySnapshot = captureMemorySnapshot();
        long queryStart = System.nanoTime();

        try {
            jdbcTemplate.query(
                    connection -> {
                        PreparedStatement statement = connection.prepareStatement(
                                CURSOR_SQL,
                                ResultSet.TYPE_FORWARD_ONLY,
                                ResultSet.CONCUR_READ_ONLY
                        );
                        statement.setFetchSize(FETCH_SIZE);
                        return statement;
                    },
                    resultSet -> {
                        try {
                            long batchRows = 0;
                            while (resultSet.next()) {
                                long writeStart = System.nanoTime();
                                writeResultSetRow(sheet, (int) rowIndex.getAndIncrement(), resultSet);
                                accumulator.recordWrite(1, System.nanoTime() - writeStart);
                                batchRows++;

                                if (batchRows % SHEET_FLUSH_INTERVAL == 0) {
                                    sheet.flush();
                                }
                            }

                            sheet.flush();
                            long queryNanos = System.nanoTime() - queryStart;
                            MemorySnapshot afterQuerySnapshot = captureMemorySnapshot();
                            accumulator.recordQuery(batchRows, queryNanos);
                            log.info(
                                    "order-fastexcel-jdbc load mode=fastexcel-jdbc-cursor batchStartAfterId={} rows={} queryMs={} heapBeforeLoadMb={} heapAfterLoadMb={} heapLoadDeltaMb={} gcCountDuringLoad={} gcTimeMsDuringLoad={}",
                                    null,
                                    batchRows,
                                    nanosToMillis(queryNanos),
                                    round(bytesToMb(beforeQuerySnapshot.heapUsedBytes())),
                                    round(bytesToMb(afterQuerySnapshot.heapUsedBytes())),
                                    round(bytesToMb(afterQuerySnapshot.heapUsedBytes() - beforeQuerySnapshot.heapUsedBytes())),
                                    afterQuerySnapshot.gcCount() - beforeQuerySnapshot.gcCount(),
                                    afterQuerySnapshot.gcTimeMs() - beforeQuerySnapshot.gcTimeMs()
                            );
                            return null;
                        } catch (IOException exception) {
                            throw new UncheckedIOException(exception);
                        }
                    }
            );
        } catch (UncheckedIOException exception) {
            throw exception.getCause();
        }
    }

    private void writeByKeysetBatch(Worksheet sheet, BenchmarkAccumulator accumulator) throws IOException {
        AtomicLong rowIndex = new AtomicLong(1);
        Long lastIdExclusive = null;

        while (true) {
            Long batchStartAfterId = lastIdExclusive;
            long batchStartId = batchStartAfterId == null ? 0L : batchStartAfterId;
            AtomicLong batchRows = new AtomicLong();
            AtomicLong lastLoadedId = new AtomicLong(batchStartId);
            MemorySnapshot beforeQuerySnapshot = captureMemorySnapshot();
            long queryStart = System.nanoTime();

            try {
                jdbcTemplate.query(
                        connection -> {
                            PreparedStatement statement = connection.prepareStatement(
                                    KEYSET_BATCH_SQL,
                                    ResultSet.TYPE_FORWARD_ONLY,
                                    ResultSet.CONCUR_READ_ONLY
                            );
                            statement.setLong(1, batchStartId);
                            statement.setInt(2, BATCH_SIZE);
                            statement.setFetchSize(FETCH_SIZE);
                            return statement;
                        },
                        resultSet -> {
                            try {
                                while (resultSet.next()) {
                                    long writeStart = System.nanoTime();
                                    writeResultSetRow(sheet, (int) rowIndex.getAndIncrement(), resultSet);
                                    accumulator.recordWrite(1, System.nanoTime() - writeStart);
                                    long currentId = resultSet.getLong(1);
                                    lastLoadedId.set(currentId);
                                    batchRows.incrementAndGet();
                                }
                                return null;
                            } catch (IOException exception) {
                                throw new UncheckedIOException(exception);
                            }
                        }
                );
            } catch (UncheckedIOException exception) {
                throw exception.getCause();
            }

            long queryNanos = System.nanoTime() - queryStart;
            MemorySnapshot afterQuerySnapshot = captureMemorySnapshot();

            log.info(
                    "order-fastexcel-jdbc load mode=fastexcel-jdbc-keyset-batch batchStartAfterId={} rows={} queryMs={} heapBeforeLoadMb={} heapAfterLoadMb={} heapLoadDeltaMb={} gcCountDuringLoad={} gcTimeMsDuringLoad={}",
                    batchStartAfterId,
                    batchRows.get(),
                    nanosToMillis(queryNanos),
                    round(bytesToMb(beforeQuerySnapshot.heapUsedBytes())),
                    round(bytesToMb(afterQuerySnapshot.heapUsedBytes())),
                    round(bytesToMb(afterQuerySnapshot.heapUsedBytes() - beforeQuerySnapshot.heapUsedBytes())),
                    afterQuerySnapshot.gcCount() - beforeQuerySnapshot.gcCount(),
                    afterQuerySnapshot.gcTimeMs() - beforeQuerySnapshot.gcTimeMs()
            );

            if (batchRows.get() == 0) {
                break;
            }

            accumulator.recordQuery(batchRows.get(), queryNanos);
            lastIdExclusive = lastLoadedId.get();
            sheet.flush();
        }
    }

    private void writeResultSetRow(Worksheet sheet, int rowIndex, ResultSet resultSet) throws SQLException, IOException {
        sheet.value(rowIndex, 0, getNullableLong(resultSet, 1));
        writeInlineString(sheet, rowIndex, 1, resultSet.getString(2));
        sheet.value(rowIndex, 2, getNullableLong(resultSet, 3));
        sheet.value(rowIndex, 3, resultSet.getString(4));
        sheet.value(rowIndex, 4, getNullableDouble(resultSet.getBigDecimal(5)));
        writeInlineString(sheet, rowIndex, 5, formatDateTime(resultSet.getObject(6, OffsetDateTime.class)));
        writeInlineString(sheet, rowIndex, 6, formatDateTime(resultSet.getObject(7, OffsetDateTime.class)));
        writeInlineString(sheet, rowIndex, 7, formatDateTime(resultSet.getObject(8, OffsetDateTime.class)));
    }

    private long countOrders() {
        Long total = jdbcTemplate.queryForObject(COUNT_SQL, Long.class);
        return total == null ? 0L : total;
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
        sheet.inlineString(rowIndex, columnIndex, value == null ? "" : value);
    }

    private Long getNullableLong(ResultSet resultSet, int columnIndex) throws SQLException {
        long value = resultSet.getLong(columnIndex);
        return resultSet.wasNull() ? null : value;
    }

    private Double getNullableDouble(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }

    private String formatDateTime(OffsetDateTime dateTime) {
        return dateTime == null ? "" : DATE_TIME_FORMATTER.format(dateTime);
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
    private interface BenchmarkWriter {
        void write(Worksheet sheet, BenchmarkAccumulator accumulator) throws IOException;
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

        private long getByteCount() {
            return byteCount;
        }
    }

    private static final class BenchmarkAccumulator {

        private final String benchmarkName;
        private final long expectedRows;
        private long streamedRows;
        private long queryCount;
        private long totalQueryNanos;
        private long totalWriteNanos;

        private BenchmarkAccumulator(String benchmarkName, long expectedRows) {
            this.benchmarkName = benchmarkName;
            this.expectedRows = expectedRows;
        }

        private void recordQuery(long rows, long queryNanos) {
            if (rows == 0) {
                return;
            }
            streamedRows += rows;
            queryCount++;
            totalQueryNanos += queryNanos;
        }

        private void recordWrite(long rows, long writeNanos) {
            totalWriteNanos += writeNanos;
        }

        private String benchmarkName() {
            return benchmarkName;
        }

        private long expectedRows() {
            return expectedRows;
        }

        private long streamedRows() {
            return streamedRows;
        }

        private long queryCount() {
            return queryCount;
        }

        private long totalQueryNanos() {
            return totalQueryNanos;
        }

        private long totalWriteNanos() {
            return totalWriteNanos;
        }
    }

    private record MemorySnapshot(
            long heapUsedBytes,
            long nonHeapUsedBytes,
            long gcCount,
            long gcTimeMs
    ) {
    }
}
