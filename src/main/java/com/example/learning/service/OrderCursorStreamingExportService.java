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

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderCursorStreamingExportService {

    private static final int JDBC_FETCH_SIZE = 1_000;
    private static final int SHEET_FLUSH_INTERVAL = 5_000;
    private static final long GC_SETTLE_NANOS = 200_000_000L;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private static final String EXPORT_SQL = """
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

    private final JdbcTemplate jdbcTemplate;

    @Transactional(readOnly = true)
    public void exportOrders(OutputStream outputStream) throws IOException {
        stabilizeHeap();
        MemorySnapshot beforeStreamingSnapshot = captureMemorySnapshot();
        long exportStart = System.nanoTime();

        CountingOutputStream countingOutputStream = new CountingOutputStream(outputStream);
        AtomicLong streamedRows = new AtomicLong();

        try (Workbook workbook = new Workbook(countingOutputStream, "learning", "1.0")) {
            Worksheet sheet = workbook.newWorksheet("orders");
            writeHeader(sheet);

            try {
                jdbcTemplate.query(
                        connection -> {
                            PreparedStatement statement = connection.prepareStatement(
                                    EXPORT_SQL,
                                    ResultSet.TYPE_FORWARD_ONLY,
                                    ResultSet.CONCUR_READ_ONLY
                            );
                            statement.setFetchSize(JDBC_FETCH_SIZE);
                            return statement;
                        },
                        resultSet -> {
                            try {
                                writeRows(sheet, resultSet, streamedRows);
                                return null;
                            } catch (IOException exception) {
                                throw new UncheckedIOException(exception);
                            }
                        }
                );
            } catch (UncheckedIOException exception) {
                throw exception.getCause();
            }

            workbook.finish();
            countingOutputStream.flush();
        }

        long exportEnd = System.nanoTime();
        MemorySnapshot afterStreamingSnapshot = captureMemorySnapshot();

        stabilizeHeap();
        MemorySnapshot afterForcedGcSnapshot = captureMemorySnapshot();

        log.info(
                "order-cursor-streaming benchmark library=fastexcel-cursor fetchSize={} flushInterval={} streamedRows={} totalMs={} fileSizeKb={} heapBeforeMb={} heapAfterMb={} heapAfterForcedGcMb={} heapTransientMb={} heapRetainedMb={} nonHeapBeforeMb={} nonHeapAfterMb={} gcCountDuringStreaming={} gcTimeMsDuringStreaming={}",
                JDBC_FETCH_SIZE,
                SHEET_FLUSH_INTERVAL,
                streamedRows.get(),
                nanosToMillis(exportEnd - exportStart),
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

    private void writeRows(Worksheet sheet, ResultSet resultSet, AtomicLong streamedRows) throws SQLException, IOException {
        int rowIndex = 1;

        while (resultSet.next()) {
            sheet.value(rowIndex, 0, getNullableLong(resultSet, 1));
            writeInlineString(sheet, rowIndex, 1, resultSet.getString(2));
            sheet.value(rowIndex, 2, getNullableLong(resultSet, 3));
            sheet.value(rowIndex, 3, resultSet.getString(4));
            sheet.value(rowIndex, 4, getNullableDouble(resultSet.getBigDecimal(5)));
            writeInlineString(sheet, rowIndex, 5, formatDateTime(getOffsetDateTime(resultSet, 6)));
            writeInlineString(sheet, rowIndex, 6, formatDateTime(getOffsetDateTime(resultSet, 7)));
            writeInlineString(sheet, rowIndex, 7, formatDateTime(getOffsetDateTime(resultSet, 8)));

            streamedRows.incrementAndGet();
            rowIndex++;

            if (rowIndex % SHEET_FLUSH_INTERVAL == 0) {
                sheet.flush();
            }
        }

        sheet.flush();
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

    private OffsetDateTime getOffsetDateTime(ResultSet resultSet, int columnIndex) throws SQLException {
        return resultSet.getObject(columnIndex, OffsetDateTime.class);
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

    private record MemorySnapshot(
            long heapUsedBytes,
            long nonHeapUsedBytes,
            long gcCount,
            long gcTimeMs
    ) {
    }
}
