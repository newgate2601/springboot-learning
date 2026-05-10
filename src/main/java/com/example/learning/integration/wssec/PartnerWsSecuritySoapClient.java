package com.example.learning.integration.wssec; // Package client WS-Security chuẩn.

import com.example.learning.generated.securetransfer.SecureTransferPort; // Port interface được generate từ secure-transfers.wsdl.
import com.example.learning.generated.securetransfer.SecureTransferPortService; // Service factory được generate từ secure-transfers.wsdl.
import com.example.learning.generated.securetransfer.SecureTransferRequest; // Request class được generate từ secure-transfers.wsdl.
import com.example.learning.generated.securetransfer.SecureTransferResponse; // Response class được generate từ secure-transfers.wsdl.
import jakarta.xml.ws.BindingProvider; // API JAX-WS để override endpoint runtime.
import org.apache.cxf.frontend.ClientProxy; // API CXF để gắn interceptor vào generated client.
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor; // CXF interceptor tự tạo WS-Security header.
import org.apache.wss4j.dom.handler.WSHandlerConstants; // Constants cấu hình action WS-Security.
import org.slf4j.Logger; // Logger interface.
import org.slf4j.LoggerFactory; // Logger factory.
import org.springframework.stereotype.Service; // Annotation Spring service.

import java.util.HashMap; // Map cấu hình WSS4J.
import java.util.Map; // Interface Map.

@Service // Service giả lập partner gọi SOAP bằng generated client + WS-Security chuẩn.
public class PartnerWsSecuritySoapClient {
    private static final Logger log = LoggerFactory.getLogger(PartnerWsSecuritySoapClient.class); // Logger flow partner WS-Security.
    private static final String SOAP_ENDPOINT = "http://localhost:8086/ws"; // Endpoint SOAP server secure.

    public SecureTransferResponse transfer(SecureTransferRequest request) { // Nhận request generated và trả response generated.
        SecureTransferPort port = createSignedGeneratedPort(); // Tạo generated SOAP port đã gắn WSS4JOutInterceptor.

        log.info("[INTEGRATOR-WSSEC] Gọi SOAP secure bằng generated client: endpoint={}, requestId={}",
                SOAP_ENDPOINT, request.getRequestId()); // Log trước khi gọi.

        SecureTransferResponse response = port.secureTransfer(request); // Method secureTransfer(...) được generate từ WSDL.

        log.info("[INTEGRATOR-WSSEC] Nhận response: transactionId={}, status={}, fee={}",
                response.getTransactionId(), response.getStatus(), response.getFee()); // Log kết quả.
        return response; // Trả response về controller.
    }

    private SecureTransferPort createSignedGeneratedPort() { // Tạo generated SOAP client và cấu hình tự ký message.
        SecureTransferPortService service = new SecureTransferPortService(); // Class service được generate từ secure-transfers.wsdl.
        SecureTransferPort port = service.getSecureTransferPortSoap11(); // Lấy SOAP 1.1 port được generate.

        BindingProvider bindingProvider = (BindingProvider) port; // Ép port sang BindingProvider để chỉnh endpoint.
        bindingProvider.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, SOAP_ENDPOINT); // Override endpoint runtime.

        ClientProxy.getClient(port).getOutInterceptors().add(new WSS4JOutInterceptor(wsSecurityProperties())); // Gắn interceptor tự thêm wsse:Security.
        return port; // Trả port đã tự ký request.
    }

    private Map<String, Object> wsSecurityProperties() { // Cấu hình cho WSS4JOutInterceptor.
        Map<String, Object> properties = new HashMap<>(); // Tạo map cấu hình.
        properties.put(WSHandlerConstants.ACTION, WSHandlerConstants.TIMESTAMP + " " + WSHandlerConstants.SIGNATURE); // Tự thêm Timestamp và Signature.
        properties.put(WSHandlerConstants.USER, "partner"); // Alias private key trong partner-keystore.jks.
        properties.put(WSHandlerConstants.PW_CALLBACK_CLASS, PartnerKeystorePasswordCallback.class.getName()); // Class trả password private key.
        properties.put(WSHandlerConstants.SIG_PROP_FILE, "security/partner-wss4j.properties"); // File cấu hình keystore để ký.
        properties.put(WSHandlerConstants.SIG_KEY_ID, "DirectReference"); // Gửi certificate reference trực tiếp trong WS-Security header.
        return properties; // Trả cấu hình cho WSS4J.
    }
}
