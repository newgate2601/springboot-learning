package com.example.learning.service;

import java.io.BufferedWriter;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static java.nio.charset.StandardCharsets.UTF_8;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderCsvJdbcBenchmarkService {

    private static final int BATCH_SIZE = 50_000;
    private static final int FETCH_SIZE = 50_000;
    private static final long GC_SETTLE_NANOS = 200_000_000L;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final String CSV_ENTRY_NAME = "orders.csv";
    private static final String LINE_SEPARATOR = "\n";

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
            order by o.id
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
            order by o.id
            limit ?
            """;

    private final JdbcTemplate jdbcTemplate;

    @Transactional(readOnly = true)
    public void exportCsvByCursor(OutputStream outputStream) throws IOException {
        export("csv-jdbc-cursor", CompressionMode.PLAIN, outputStream, this::writeCsvByCursor);
    }

    @Transactional(readOnly = true)
    public void exportCsvByKeysetBatch(OutputStream outputStream) throws IOException {
        export("csv-jdbc-keyset-batch", CompressionMode.PLAIN, outputStream, this::writeCsvByKeysetBatch);
    }

    @Transactional(readOnly = true)
    public void exportZipCsvByCursor(OutputStream outputStream) throws IOException {
        export("zip-csv-jdbc-cursor", CompressionMode.ZIP, outputStream, this::writeCsvByCursor);
    }

    @Transactional(readOnly = true)
    public void exportZipCsvByKeysetBatch(OutputStream outputStream) throws IOException {
        export("zip-csv-jdbc-keyset-batch", CompressionMode.ZIP, outputStream, this::writeCsvByKeysetBatch);
    }

    private void export(
            String benchmarkName,
            CompressionMode compressionMode,
            OutputStream outputStream,
            CsvBenchmarkWriter writer
    ) throws IOException {
        stabilizeHeap();
        MemorySnapshot beforeStreamingSnapshot = captureMemorySnapshot();
        long benchmarkStart = System.nanoTime();
        long expectedRows = countOrders();

        CountingOutputStream countingOutputStream = new CountingOutputStream(outputStream);
        BenchmarkAccumulator accumulator = new BenchmarkAccumulator(benchmarkName, compressionMode.label, expectedRows);

        if (compressionMode == CompressionMode.ZIP) {
            try (ZipOutputStream zipOutputStream = new ZipOutputStream(countingOutputStream, UTF_8);
                 BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(zipOutputStream, UTF_8), 1024 * 64)) {
                zipOutputStream.putNextEntry(new ZipEntry(CSV_ENTRY_NAME));
                writeHeader(bufferedWriter);
                writer.write(bufferedWriter, accumulator);
                bufferedWriter.flush();
                zipOutputStream.closeEntry();
                zipOutputStream.finish();
            }
        } else {
            try (BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(countingOutputStream, UTF_8), 1024 * 64)) {
                writeHeader(bufferedWriter);
                writer.write(bufferedWriter, accumulator);
                bufferedWriter.flush();
            }
        }

        long benchmarkEnd = System.nanoTime();
        MemorySnapshot afterStreamingSnapshot = captureMemorySnapshot();

        stabilizeHeap();
        MemorySnapshot afterForcedGcSnapshot = captureMemorySnapshot();

        log.info(
                "order-csv-jdbc benchmark mode={} compression={} expectedRows={} streamedRows={} queries={} batchSize={} fetchSize={} totalMs={} queryMs={} writeMs={} fileSizeKb={} heapBeforeMb={} heapAfterMb={} heapAfterForcedGcMb={} heapTransientMb={} heapRetainedMb={} nonHeapBeforeMb={} nonHeapAfterMb={} gcCountDuringStreaming={} gcTimeMsDuringStreaming={}",
                accumulator.benchmarkName(),
                accumulator.compression(),
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

    private void writeCsvByCursor(BufferedWriter writer, BenchmarkAccumulator accumulator) throws IOException {
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
                            long rows = 0;
                            while (resultSet.next()) {
                                long writeStart = System.nanoTime();
                                writeCsvRow(writer, resultSet);
                                accumulator.recordWrite(System.nanoTime() - writeStart);
                                rows++;
                            }

                            writer.flush();
                            long queryNanos = System.nanoTime() - queryStart;
                            MemorySnapshot afterQuerySnapshot = captureMemorySnapshot();
                            accumulator.recordQuery(rows, queryNanos);
                            log.info(
                                    "order-csv-jdbc load mode=csv-jdbc-cursor compression={} batchStartAfterId={} rows={} queryMs={} heapBeforeLoadMb={} heapAfterLoadMb={} heapLoadDeltaMb={} gcCountDuringLoad={} gcTimeMsDuringLoad={}",
                                    accumulator.compression(),
                                    null,
                                    rows,
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

    private void writeCsvByKeysetBatch(BufferedWriter writer, BenchmarkAccumulator accumulator) throws IOException {
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
                                    writeCsvRow(writer, resultSet);
                                    accumulator.recordWrite(System.nanoTime() - writeStart);
                                    lastLoadedId.set(resultSet.getLong(1));
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

            writer.flush();
            long queryNanos = System.nanoTime() - queryStart;
            MemorySnapshot afterQuerySnapshot = captureMemorySnapshot();

            log.info(
                    "order-csv-jdbc load mode=csv-jdbc-keyset-batch compression={} batchStartAfterId={} rows={} queryMs={} heapBeforeLoadMb={} heapAfterLoadMb={} heapLoadDeltaMb={} gcCountDuringLoad={} gcTimeMsDuringLoad={}",
                    accumulator.compression(),
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
        }
    }

    private void writeHeader(BufferedWriter writer) throws IOException {
        writer.write("id,orderNo,customerId,status,totalAmount,orderDate,createdAt,updatedAt");
        writer.write(LINE_SEPARATOR);
    }

    private void writeCsvRow(BufferedWriter writer, ResultSet resultSet) throws SQLException, IOException {
        writer.write(csvCell(getNullableLong(resultSet, 1)));
        writer.write(',');
        writer.write(csvCell(resultSet.getString(2)));
        writer.write(',');
        writer.write(csvCell(getNullableLong(resultSet, 3)));
        writer.write(',');
        writer.write(csvCell(resultSet.getString(4)));
        writer.write(',');
        writer.write(csvCell(resultSet.getBigDecimal(5)));
        writer.write(',');
        writer.write(csvCell(formatDateTime(resultSet.getObject(6, OffsetDateTime.class))));
        writer.write(',');
        writer.write(csvCell(formatDateTime(resultSet.getObject(7, OffsetDateTime.class))));
        writer.write(',');
        writer.write(csvCell(formatDateTime(resultSet.getObject(8, OffsetDateTime.class))));
        writer.write(LINE_SEPARATOR);
    }

    private String csvCell(Object value) {
        if (value == null) {
            return "";
        }

        String text = value.toString();
        boolean requiresQuotes = text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r");
        if (!requiresQuotes) {
            return text;
        }
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }

    private Long getNullableLong(ResultSet resultSet, int columnIndex) throws SQLException {
        long value = resultSet.getLong(columnIndex);
        return resultSet.wasNull() ? null : value;
    }

    private String formatDateTime(OffsetDateTime dateTime) {
        return dateTime == null ? "" : DATE_TIME_FORMATTER.format(dateTime);
    }

    private long countOrders() {
        Long total = jdbcTemplate.queryForObject(COUNT_SQL, Long.class);
        return total == null ? 0L : total;
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
    private interface CsvBenchmarkWriter {
        void write(BufferedWriter writer, BenchmarkAccumulator accumulator) throws IOException;
    }

    private enum CompressionMode {
        PLAIN("plain"),
        ZIP("zip");

        private final String label;

        CompressionMode(String label) {
            this.label = label;
        }
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
        private final String compression;
        private final long expectedRows;
        private long streamedRows;
        private long queryCount;
        private long totalQueryNanos;
        private long totalWriteNanos;

        private BenchmarkAccumulator(String benchmarkName, String compression, long expectedRows) {
            this.benchmarkName = benchmarkName;
            this.compression = compression;
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

        private void recordWrite(long writeNanos) {
            totalWriteNanos += writeNanos;
        }

        private String benchmarkName() {
            return benchmarkName;
        }

        private String compression() {
            return compression;
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
