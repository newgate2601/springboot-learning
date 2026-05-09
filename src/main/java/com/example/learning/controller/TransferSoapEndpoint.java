package com.example.learning.controller;

import com.example.learning.security.PartnerSecurityService;
import com.example.learning.soap.SoapTransferRequest;
import com.example.learning.soap.SoapTransferResponse;
import com.example.learning.transfer.PartnerTransferService;
import com.example.learning.transfer.TransferCommand;
import com.example.learning.transfer.TransferResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;
import org.springframework.ws.soap.SoapHeaderElement;
import org.springframework.ws.soap.server.endpoint.annotation.SoapHeader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.TransformerFactory;

@Endpoint // SOAP endpoint: Spring-WS sẽ route SOAP XML request vào class này.
public class TransferSoapEndpoint {
    private static final Logger log = LoggerFactory.getLogger(TransferSoapEndpoint.class); // Logger để nhìn flow SOAP trong console.

    private static final String TRANSFER_NAMESPACE = "http://example.com/learning/transfer"; // Namespace của SOAP body.
    private static final String SECURITY_NAMESPACE = "http://example.com/learning/security"; // Namespace của SOAP security header demo.
    private static final String SECURITY_HEADER = "{" + SECURITY_NAMESPACE + "}PartnerSecurity"; // Header cần đọc trong SOAP Envelope.

    private final PartnerTransferService transferService; // Service xử lý nghiệp vụ dùng chung REST/SOAP.
    private final PartnerSecurityService securityService; // Service validate security dùng chung REST/SOAP.

    // Constructor injection: Spring tự truyền bean PartnerTransferService và PartnerSecurityService vào đây.
    public TransferSoapEndpoint(PartnerTransferService transferService, PartnerSecurityService securityService) {
        this.transferService = transferService;
        this.securityService = securityService;
    }

    @PayloadRoot(namespace = TRANSFER_NAMESPACE, localPart = "TransferRequest") // Bắt SOAP Body có root element là TransferRequest.
    @ResponsePayload // Object return sẽ được Spring marshal thành SOAP Body response.
    public SoapTransferResponse transfer(
            @RequestPayload SoapTransferRequest request, // SOAP Body XML được map thành object Java.
            @SoapHeader(SECURITY_HEADER) SoapHeaderElement securityHeader // SOAP Header chứa clientId/timestamp/signature.
    ) {
        log.info("[SOAP] Nhận TransferRequest: requestId={}, from={}, to={}, amount={}, currency={}",
                request.getRequestId(), request.getFromAccount(), request.getToAccount(), request.getAmount(), request.getCurrency());

        // SOAP Header là XML riêng nên cần parse ra object để lấy các trường security.
        PartnerSecurityHeader header = readSecurityHeader(securityHeader);
        log.info("[SOAP] Đọc SOAP Header xong: clientId={}, timestamp={}", header.clientId, header.timestamp);

        // Dùng cùng cách tạo canonical payload như REST để so sánh 2 kiểu API công bằng.
        String canonicalPayload = securityService.canonicalPayload(
                request.getRequestId(),
                request.getAmount().toPlainString(),
                request.getCurrency()
        );
        log.info("[SOAP] Canonical payload dùng để verify signature: {}", canonicalPayload);

        // Kiểm tra client, timestamp và chữ ký HMAC lấy từ SOAP Header.
        securityService.validate(header.clientId, header.timestamp, header.signature, canonicalPayload);
        log.info("[SOAP] Security hợp lệ, bắt đầu gọi business service");

        // Sau khi hợp lệ, SOAP cũng gọi đúng business service như REST.
        SoapTransferResponse response = toResponse(transferService.transfer(toCommand(request)));
        log.info("[SOAP] Trả response: transactionId={}, status={}, fee={}",
                response.getTransactionId(), response.getStatus(), response.getFee());
        return response;
    }

    // Map SOAP DTO về command nội bộ để service không phụ thuộc SOAP/JAXB.
    private TransferCommand toCommand(SoapTransferRequest request) {
        TransferCommand command = new TransferCommand();
        command.setRequestId(request.getRequestId());
        command.setFromAccount(request.getFromAccount());
        command.setToAccount(request.getToAccount());
        command.setAmount(request.getAmount());
        command.setCurrency(request.getCurrency());
        return command;
    }

    // Map kết quả nghiệp vụ chung về SOAP response DTO để Spring marshal thành XML.
    private SoapTransferResponse toResponse(TransferResult result) {
        SoapTransferResponse response = new SoapTransferResponse();
        response.setTransactionId(result.getTransactionId());
        response.setRequestId(result.getRequestId());
        response.setStatus(result.getStatus());
        response.setAmount(result.getAmount());
        response.setFee(result.getFee());
        response.setCurrency(result.getCurrency());
        response.setMessage(result.getMessage());
        return response;
    }

    // Demo này đọc security header thủ công để thấy SOAP Header nằm ở đâu.
    // Trong WS-Security production, phần này thường được thay bằng WSS4J interceptor/policy.
    private PartnerSecurityHeader readSecurityHeader(SoapHeaderElement securityHeader) {
        try {
            DOMResult result = new DOMResult(); // Nơi chứa XML header sau khi transform.
            TransformerFactory.newInstance().newTransformer().transform(securityHeader.getSource(), result); // Chuyển SOAP header source thành DOM.
            Document document = (Document) result.getNode(); // Lấy document XML từ DOMResult.
            Element root = document.getDocumentElement(); // Root là <sec:PartnerSecurity>.
            return new PartnerSecurityHeader(
                    childText(root, "clientId"), // Lấy <sec:clientId>.
                    childText(root, "timestamp"), // Lấy <sec:timestamp>.
                    childText(root, "signature") // Lấy <sec:signature>.
            );
        } catch (Exception ex) {
            throw new SecurityException("Invalid SOAP security header", ex);
        }
    }

    // Helper đọc text của child XML element theo local name.
    private String childText(Element root, String localName) {
        // Ưu tiên tìm theo namespace đầy đủ: http://example.com/learning/security.
        Element element = (Element) root.getElementsByTagNameNS(SECURITY_NAMESPACE, localName).item(0);
        if (element == null) {
            // Fallback cho request demo không khai báo namespace đúng cách.
            element = (Element) root.getElementsByTagName(localName).item(0);
        }
        if (element == null) {
            throw new SecurityException("Missing SOAP security header field: " + localName);
        }
        return element.getTextContent();
    }

    // Object nội bộ để gom 3 giá trị security đọc từ SOAP Header.
    private static class PartnerSecurityHeader {
        private final String clientId;
        private final String timestamp;
        private final String signature;

        private PartnerSecurityHeader(String clientId, String timestamp, String signature) {
            this.clientId = clientId;
            this.timestamp = timestamp;
            this.signature = signature;
        }
    }
}
