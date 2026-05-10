package com.example.learning.soap.secure; // Package chứa DTO SOAP response cho flow WS-Security.

import jakarta.xml.bind.annotation.XmlAccessType; // Chọn cách JAXB truy cập field/getter.
import jakarta.xml.bind.annotation.XmlAccessorType; // Annotation cấu hình JAXB access.
import jakarta.xml.bind.annotation.XmlElement; // Annotation map field Java với XML element.
import jakarta.xml.bind.annotation.XmlRootElement; // Annotation map class với root XML element.

import java.math.BigDecimal; // Kiểu tiền tệ.

@XmlRootElement(name = "SecureTransferResponse", namespace = SecureSoapTransferResponse.NAMESPACE) // Root XML response secure.
@XmlAccessorType(XmlAccessType.FIELD) // JAXB marshal/unmarshal trực tiếp field.
public class SecureSoapTransferResponse {
    public static final String NAMESPACE = "http://example.com/learning/secure-transfer"; // Namespace phải khớp XSD/WSDL secure.

    @XmlElement(namespace = NAMESPACE, required = true)
    private String transactionId; // Mã giao dịch.

    @XmlElement(namespace = NAMESPACE, required = true)
    private String requestId; // Mã request.

    @XmlElement(namespace = NAMESPACE, required = true)
    private String status; // Trạng thái.

    @XmlElement(namespace = NAMESPACE, required = true)
    private BigDecimal amount; // Số tiền.

    @XmlElement(namespace = NAMESPACE, required = true)
    private BigDecimal fee; // Phí.

    @XmlElement(namespace = NAMESPACE, required = true)
    private String currency; // Loại tiền.

    @XmlElement(namespace = NAMESPACE, required = true)
    private String message; // Message kết quả.

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
