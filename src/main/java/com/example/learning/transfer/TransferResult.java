package com.example.learning.transfer;

import java.math.BigDecimal;

// Result nội bộ của nghiệp vụ. Controller/endpoint sẽ map result này ra REST JSON hoặc SOAP XML.
public class TransferResult {
    private String transactionId; // Mã giao dịch server sinh ra.
    private String requestId; // Mã request client gửi, trả lại để client đối chiếu.
    private String status; // Trạng thái xử lý, demo là ACCEPTED.
    private BigDecimal amount; // Số tiền giao dịch.
    private BigDecimal fee; // Phí giao dịch.
    private String currency; // Loại tiền.
    private String message; // Message để dễ test/đọc ví dụ.

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getFee() {
        return fee;
    }

    public void setFee(BigDecimal fee) {
        this.fee = fee;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
