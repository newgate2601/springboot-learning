package com.example.learning.soap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.math.BigDecimal;

@XmlRootElement(name = "TransferRequest", namespace = SoapTransferRequest.NAMESPACE) // Root element trong SOAP Body là <TransferRequest>.
@XmlAccessorType(XmlAccessType.FIELD) // JAXB map XML trực tiếp vào field, không cần annotation trên getter.
public class SoapTransferRequest {
    public static final String NAMESPACE = "http://example.com/learning/transfer"; // Namespace phải khớp XSD/WSDL.

    @XmlElement(namespace = NAMESPACE, required = true) // Field bắt buộc trong XML và thuộc namespace transfer.
    private String requestId; // Mã request client gửi.

    @XmlElement(namespace = NAMESPACE, required = true)
    private String fromAccount; // Tài khoản nguồn.

    @XmlElement(namespace = NAMESPACE, required = true)
    private String toAccount; // Tài khoản đích.

    @XmlElement(namespace = NAMESPACE, required = true)
    private BigDecimal amount; // Số tiền chuyển.

    @XmlElement(namespace = NAMESPACE, required = true)
    private String currency; // Loại tiền.

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
