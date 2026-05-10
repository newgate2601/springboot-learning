package com.example.learning.controller; // Package controller REST.

import com.example.learning.dto.TransferRestRequest; // DTO JSON để test bằng Postman.
import com.example.learning.generated.securetransfer.SecureTransferRequest; // Request generated từ secure-transfers.wsdl.
import com.example.learning.generated.securetransfer.SecureTransferResponse; // Response generated từ secure-transfers.wsdl.
import com.example.learning.integration.wssec.PartnerWsSecuritySoapClient; // Client generated + WS-Security.
import lombok.AllArgsConstructor; // Lombok constructor injection.
import org.springframework.web.bind.annotation.PostMapping; // Annotation HTTP POST.
import org.springframework.web.bind.annotation.RequestBody; // Annotation đọc JSON body.
import org.springframework.web.bind.annotation.RequestMapping; // Annotation base path.
import org.springframework.web.bind.annotation.RestController; // Annotation REST controller.

@RestController // REST API giả lập partner dùng WS-Security chuẩn để gọi SOAP server.
@RequestMapping("/api/v1/integrator-wssec") // Base path riêng để không đụng API partner cũ.
@AllArgsConstructor // Tự sinh constructor inject client.
public class PartnerWsSecurityIntegrationController {
    private final PartnerWsSecuritySoapClient soapClient; // Client dùng generated code + WSS4J tự ký.

    @PostMapping("/transfer-via-soap") // API test: POST /api/v1/integrator-wssec/transfer-via-soap.
    public SecureTransferResponse transferViaSoap(@RequestBody TransferRestRequest request) { // Nhận JSON, trả response generated.
        SecureTransferRequest soapRequest = new SecureTransferRequest(); // Class generated từ WSDL secure.
        soapRequest.setRequestId(request.getRequestId()); // Map requestId.
        soapRequest.setFromAccount(request.getFromAccount()); // Map tài khoản nguồn.
        soapRequest.setToAccount(request.getToAccount()); // Map tài khoản đích.
        soapRequest.setAmount(request.getAmount()); // Map số tiền.
        soapRequest.setCurrency(request.getCurrency()); // Map loại tiền.
        return soapClient.transfer(soapRequest); // Gọi SOAP secure, client tự ký WS-Security.
    }
}
