package com.example.learning.dto;

import java.math.BigDecimal;

// DTO nhận JSON body của REST API POST /api/v1/partner-transfer.
public class TransferRestRequest {
    private String requestId; // Mã request client gửi.
    private String fromAccount; // Tài khoản nguồn.
    private String toAccount; // Tài khoản đích.
    private BigDecimal amount; // Số tiền, dùng BigDecimal cho tiền tệ.
    private String currency; // Loại tiền, ví dụ VND.

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getFromAccount() {
        return fromAccount;
    }

    public void setFromAccount(String fromAccount) {
        this.fromAccount = fromAccount;
    }

    public String getToAccount() {
        return toAccount;
    }

    public void setToAccount(String toAccount) {
        this.toAccount = toAccount;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
