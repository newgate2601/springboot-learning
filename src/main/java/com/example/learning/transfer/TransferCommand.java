package com.example.learning.transfer;

import java.math.BigDecimal;

// Command nội bộ của nghiệp vụ. REST DTO và SOAP DTO đều được map về class này.
public class TransferCommand {
    private String requestId; // Mã request do client gửi, dùng để trace/idempotency demo.
    private String fromAccount; // Tài khoản nguồn.
    private String toAccount; // Tài khoản đích.
    private BigDecimal amount; // Số tiền, dùng BigDecimal vì đây là dữ liệu tiền tệ.
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
