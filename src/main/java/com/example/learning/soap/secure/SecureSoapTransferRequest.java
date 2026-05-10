package com.example.learning.soap.secure; // Package chứa DTO SOAP server cho flow WS-Security.

import jakarta.xml.bind.annotation.XmlAccessType; // Chọn cách JAXB truy cập field/getter.
import jakarta.xml.bind.annotation.XmlAccessorType; // Annotation cấu hình JAXB access.
import jakarta.xml.bind.annotation.XmlElement; // Annotation map field Java với XML element.
import jakarta.xml.bind.annotation.XmlRootElement; // Annotation map class với root XML element.

import java.math.BigDecimal; // Kiểu dữ liệu tiền tệ.

@XmlRootElement(name = "SecureTransferRequest", namespace = SecureSoapTransferRequest.NAMESPACE) // Root XML của request secure.
@XmlAccessorType(XmlAccessType.FIELD) // JAXB đọc/ghi trực tiếp field.
public class SecureSoapTransferRequest {
    public static final String NAMESPACE = "http://example.com/learning/secure-transfer"; // Namespace phải khớp XSD/WSDL secure.

    @XmlElement(namespace = NAMESPACE, required = true)
    private String requestId; // Mã request.

    @XmlElement(namespace = NAMESPACE, required = true)
    private String fromAccount; // Tài khoản nguồn.

    @XmlElement(namespace = NAMESPACE, required = true)
    private String toAccount; // Tài khoản đích.

    @XmlElement(namespace = NAMESPACE, required = true)
    private BigDecimal amount; // Số tiền.

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
