package com.example.learning.transfer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service // Business service: REST và SOAP đều gọi vào đây.
public class PartnerTransferService {
    private static final Logger log = LoggerFactory.getLogger(PartnerTransferService.class); // Logger để nhìn bước xử lý nghiệp vụ.

    private static final BigDecimal FEE_RATE = new BigDecimal("0.002"); // Phí demo = 0.2% số tiền chuyển.
    private static final BigDecimal MIN_AMOUNT = new BigDecimal("10000"); // Số tiền tối thiểu để chấp nhận giao dịch.

    // Method nghiệp vụ không biết request đến từ REST hay SOAP.
    public TransferResult transfer(TransferCommand command) {
        log.info("[BUSINESS] Bắt đầu xử lý chuyển tiền: requestId={}, from={}, to={}, amount={}, currency={}",
                command.getRequestId(), command.getFromAccount(), command.getToAccount(), command.getAmount(), command.getCurrency());

        // BigDecimal dùng cho tiền tệ để tránh lỗi làm tròn của double/float.
        if (command.getAmount() == null || command.getAmount().compareTo(MIN_AMOUNT) < 0) {
            log.warn("[BUSINESS] Reject giao dịch vì amount không hợp lệ: {}", command.getAmount());
            throw new IllegalArgumentException("Amount must be at least 10000");
        }

        TransferResult result = new TransferResult(); // Tạo object kết quả trả về cho controller/endpoint.
        result.setTransactionId("TX-" + UUID.randomUUID()); // Sinh transaction id demo.
        result.setRequestId(command.getRequestId()); // Giữ lại request id để client đối chiếu.
        result.setStatus("ACCEPTED"); // Demo chỉ đánh dấu đã chấp nhận xử lý, chưa trừ tiền thật.
        result.setAmount(command.getAmount()); // Trả lại số tiền request.
        result.setFee(command.getAmount().multiply(FEE_RATE).setScale(0, RoundingMode.HALF_UP)); // Tính phí và làm tròn.
        result.setCurrency(command.getCurrency()); // Trả lại loại tiền.
        result.setMessage("Transfer accepted from " + command.getFromAccount() + " to " + command.getToAccount()); // Message để dễ đọc khi test.
        log.info("[BUSINESS] Xử lý xong: transactionId={}, fee={}, status={}",
                result.getTransactionId(), result.getFee(), result.getStatus());
        return result;
    }
}
