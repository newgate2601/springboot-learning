package com.example.learning.integration; // Package chứa code giả lập bên tích hợp.

import com.example.learning.generated.transfer.TransferPort; // Interface port được CXF generate từ WSDL.
import com.example.learning.generated.transfer.TransferPortService; // Service factory được CXF generate từ WSDL.
import com.example.learning.generated.transfer.TransferRequest; // Request class được CXF generate từ WSDL.
import com.example.learning.generated.transfer.TransferResponse; // Response class được CXF generate từ WSDL.
import com.example.learning.security.PartnerSecurityService; // Service dùng chung để tạo canonical payload và HMAC signature.
import jakarta.xml.ws.BindingProvider; // API JAX-WS dùng để override endpoint runtime.
import org.apache.cxf.frontend.ClientProxy; // API CXF dùng để lấy client proxy và gắn interceptor.
import org.slf4j.Logger; // Interface logger chuẩn.
import org.slf4j.LoggerFactory; // Factory tạo logger.
import org.springframework.stereotype.Service; // Annotation đăng ký Spring service.

@Service // Service này giả lập code phía partner dùng client được generate từ WSDL.
public class PartnerTransferSoapClient {
    private static final Logger log = LoggerFactory.getLogger(PartnerTransferSoapClient.class); // Logger để nhìn flow phía partner.
    private static final String SOAP_ENDPOINT = "http://localhost:8086/ws"; // URL SOAP endpoint thật của hệ thống mình.
    private static final String CLIENT_ID = "partner-mobile"; // Client id demo để gửi trong SOAP Header.
    private static final String DEMO_TIMESTAMP = "2125-05-09T00:00:00Z"; // Timestamp demo xa tương lai để test không bị hết hạn.

    private final PartnerSecurityService securityService; // Service tạo chữ ký giống công thức server sẽ verify.

    public PartnerTransferSoapClient(PartnerSecurityService securityService) { // Constructor injection.
        this.securityService = securityService; // Gán dependency vào field.
    }

    public TransferResponse transfer(TransferRequest request) { // Nhận request generated và trả response generated.
        String canonicalPayload = securityService.canonicalPayload( // Tạo chuỗi chuẩn để ký.
                request.getRequestId(), // Thành phần 1: requestId.
                request.getAmount().toPlainString(), // Thành phần 2: amount dạng text ổn định.
                request.getCurrency() // Thành phần 3: currency.
        );
        String signature = securityService.sign(CLIENT_ID, DEMO_TIMESTAMP, canonicalPayload); // Tạo HMAC signature cho SOAP Header.

        TransferPort port = createGeneratedSoapPort(signature); // Tạo port generated và gắn SOAP Header security.

        log.info("[INTEGRATOR] Gọi SOAP bằng generated client: endpoint={}, requestId={}, canonicalPayload={}",
                SOAP_ENDPOINT, request.getRequestId(), canonicalPayload); // Log trước khi gọi SOAP.

        TransferResponse response = port.transfer(request); // Method transfer(...) này được CXF generate từ operation trong WSDL.

        log.info("[INTEGRATOR] Nhận SOAP response từ generated client: transactionId={}, status={}, fee={}",
                response.getTransactionId(), response.getStatus(), response.getFee()); // Log kết quả nhận từ SOAP server.
        return response; // Trả response về controller giả lập partner.
    }

    private TransferPort createGeneratedSoapPort(String signature) { // Tạo SOAP client proxy từ code generated.
        TransferPortService service = new TransferPortService(); // Class service này được CXF sinh từ transfers.wsdl.
        TransferPort port = service.getTransferPortSoap11(); // Lấy port SOAP 1.1 được khai báo trong WSDL.

        BindingProvider bindingProvider = (BindingProvider) port; // Ép port sang BindingProvider để chỉnh endpoint runtime.
        bindingProvider.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, SOAP_ENDPOINT); // Override endpoint sang localhost.

        ClientProxy.getClient(port) // Lấy CXF client bên dưới generated proxy.
                .getOutInterceptors() // Lấy danh sách interceptor chạy khi gửi request ra ngoài.
                .add(new PartnerSecurityHeaderOutInterceptor(CLIENT_ID, DEMO_TIMESTAMP, signature)); // Gắn interceptor thêm SOAP Header.
        return port; // Trả port đã sẵn sàng gọi SOAP.
    }
}
