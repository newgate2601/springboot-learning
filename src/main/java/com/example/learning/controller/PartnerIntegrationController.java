package com.example.learning.controller; // Package chứa các controller REST của ứng dụng.

import com.example.learning.dto.TransferRestRequest; // DTO JSON nhận từ Postman khi test API giả lập partner.
import com.example.learning.generated.transfer.TransferRequest; // Class request được CXF generate từ file WSDL.
import com.example.learning.generated.transfer.TransferResponse; // Class response được CXF generate từ file WSDL.
import com.example.learning.integration.PartnerTransferSoapClient; // Client giả lập bên tích hợp dùng để gọi SOAP server.
import lombok.AllArgsConstructor; // Lombok tự sinh constructor cho field final.
import org.springframework.web.bind.annotation.PostMapping; // Annotation khai báo API HTTP POST.
import org.springframework.web.bind.annotation.RequestBody; // Annotation lấy JSON body map vào object Java.
import org.springframework.web.bind.annotation.RequestMapping; // Annotation khai báo base path cho controller.
import org.springframework.web.bind.annotation.RestController; // Annotation khai báo REST controller.

@RestController // Đây là REST API giả lập hệ thống bên thứ ba đang tích hợp vào SOAP service của mình.
@RequestMapping("/api/v1/integrator") // Base path: mọi API trong class này bắt đầu bằng /api/v1/integrator.
@AllArgsConstructor // Tự sinh constructor để Spring inject PartnerTransferSoapClient.
public class PartnerIntegrationController {
    private final PartnerTransferSoapClient soapClient; // Client SOAP phía partner, bên trong dùng code generated từ WSDL.

    @PostMapping("/transfer-via-soap") // API test: POST /api/v1/integrator/transfer-via-soap.
    public TransferResponse transferViaSoap(@RequestBody TransferRestRequest request) { // Nhận JSON từ Postman, trả response generated từ SOAP.
        TransferRequest soapRequest = new TransferRequest(); // Class này không tự viết tay, Maven generate từ transfers.wsdl.
        soapRequest.setRequestId(request.getRequestId()); // Map requestId từ JSON sang SOAP request generated.
        soapRequest.setFromAccount(request.getFromAccount()); // Map tài khoản nguồn.
        soapRequest.setToAccount(request.getToAccount()); // Map tài khoản đích.
        soapRequest.setAmount(request.getAmount()); // Map số tiền.
        soapRequest.setCurrency(request.getCurrency()); // Map loại tiền.
        return soapClient.transfer(soapRequest); // Gọi SOAP endpoint http://localhost:8086/ws thông qua generated client.
    }
}
