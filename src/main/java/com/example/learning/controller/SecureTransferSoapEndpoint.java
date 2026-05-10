package com.example.learning.controller; // Package controller/endpoint.

import com.example.learning.soap.secure.SecureSoapTransferRequest; // SOAP request DTO riêng cho WS-Security.
import com.example.learning.soap.secure.SecureSoapTransferResponse; // SOAP response DTO riêng cho WS-Security.
import com.example.learning.transfer.PartnerTransferService; // Service nghiệp vụ dùng chung.
import com.example.learning.transfer.TransferCommand; // Command nội bộ.
import com.example.learning.transfer.TransferResult; // Result nội bộ.
import org.slf4j.Logger; // Logger.
import org.slf4j.LoggerFactory; // Factory tạo logger.
import org.springframework.ws.server.endpoint.annotation.Endpoint; // Annotation SOAP endpoint.
import org.springframework.ws.server.endpoint.annotation.PayloadRoot; // Match SOAP Body theo namespace/localPart.
import org.springframework.ws.server.endpoint.annotation.RequestPayload; // Lấy SOAP Body request.
import org.springframework.ws.server.endpoint.annotation.ResponsePayload; // Trả SOAP Body response.

@Endpoint // Endpoint SOAP riêng cho flow chuẩn WS-Security.
public class SecureTransferSoapEndpoint {
    private static final Logger log = LoggerFactory.getLogger(SecureTransferSoapEndpoint.class); // Logger flow secure SOAP.
    private static final String NAMESPACE = "http://example.com/learning/secure-transfer"; // Namespace khớp secure-transfer.xsd.

    private final PartnerTransferService transferService; // Service nghiệp vụ dùng chung với REST/SOAP cũ.

    public SecureTransferSoapEndpoint(PartnerTransferService transferService) { // Constructor injection.
        this.transferService = transferService; // Gán service.
    }

    @PayloadRoot(namespace = NAMESPACE, localPart = "SecureTransferRequest") // Nhận request secure đúng namespace/localPart.
    @ResponsePayload // Return object sẽ được marshal thành SOAP Body response.
    public SecureSoapTransferResponse secureTransfer(@RequestPayload SecureSoapTransferRequest request) { // Method xử lý SOAP secure.
        log.info("[SOAP-WSSEC] Request đã qua WSS4J verify, bắt đầu xử lý: requestId={}, amount={}",
                request.getRequestId(), request.getAmount()); // Nếu vào được đây nghĩa là WSS4J đã verify chữ ký thành công.

        SecureSoapTransferResponse response = toResponse(transferService.transfer(toCommand(request))); // Gọi nghiệp vụ chung.

        log.info("[SOAP-WSSEC] Trả response: transactionId={}, status={}, fee={}",
                response.getTransactionId(), response.getStatus(), response.getFee()); // Log kết quả.
        return response; // Trả SOAP response.
    }

    private TransferCommand toCommand(SecureSoapTransferRequest request) { // Map SOAP DTO secure sang command nội bộ.
        TransferCommand command = new TransferCommand(); // Tạo command.
        command.setRequestId(request.getRequestId()); // Copy requestId.
        command.setFromAccount(request.getFromAccount()); // Copy tài khoản nguồn.
        command.setToAccount(request.getToAccount()); // Copy tài khoản đích.
        command.setAmount(request.getAmount()); // Copy số tiền.
        command.setCurrency(request.getCurrency()); // Copy loại tiền.
        return command; // Trả command.
    }

    private SecureSoapTransferResponse toResponse(TransferResult result) { // Map result nội bộ sang SOAP response secure.
        SecureSoapTransferResponse response = new SecureSoapTransferResponse(); // Tạo response.
        response.setTransactionId(result.getTransactionId()); // Copy transactionId.
        response.setRequestId(result.getRequestId()); // Copy requestId.
        response.setStatus(result.getStatus()); // Copy status.
        response.setAmount(result.getAmount()); // Copy amount.
        response.setFee(result.getFee()); // Copy fee.
        response.setCurrency(result.getCurrency()); // Copy currency.
        response.setMessage(result.getMessage()); // Copy message.
        return response; // Trả response.
    }
}
