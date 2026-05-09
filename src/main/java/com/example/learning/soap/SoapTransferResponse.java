package com.example.learning.soap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.math.BigDecimal;

@XmlRootElement(name = "TransferResponse", namespace = SoapTransferResponse.NAMESPACE) // Root element của SOAP response body.
@XmlAccessorType(XmlAccessType.FIELD) // JAXB marshal field Java thành XML element.
public class SoapTransferResponse {
    public static final String NAMESPACE = "http://example.com/learning/transfer"; // Namespace phải khớp XSD/WSDL.

    @XmlElement(namespace = NAMESPACE, required = true)
    private String transactionId; // Mã giao dịch server sinh ra.

    @XmlElement(namespace = NAMESPACE, required = true)
    private String requestId; // Mã request client gửi.

    @XmlElement(namespace = NAMESPACE, required = true)
    private String status; // Trạng thái giao dịch.

    @XmlElement(namespace = NAMESPACE, required = true)
    private BigDecimal amount; // Số tiền giao dịch.

    @XmlElement(namespace = NAMESPACE, required = true)
    private BigDecimal fee; // Phí giao dịch.

    @XmlElement(namespace = NAMESPACE, required = true)
    private String currency; // Loại tiền.

    @XmlElement(namespace = NAMESPACE, required = true)
    private String message; // Message mô tả kết quả.

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
