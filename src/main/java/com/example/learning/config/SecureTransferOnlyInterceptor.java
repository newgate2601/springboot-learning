package com.example.learning.config; // Package cấu hình Spring-WS.

import org.slf4j.Logger; // Logger interface.
import org.slf4j.LoggerFactory; // Factory tạo logger.
import org.springframework.ws.context.MessageContext; // Context chứa SOAP request/response.
import org.springframework.ws.server.EndpointInterceptor; // Interface interceptor của Spring-WS.
import org.springframework.ws.soap.SoapBody; // SOAP Body.
import org.springframework.ws.soap.SoapMessage; // SOAP Message.
import org.springframework.ws.WebServiceMessage; // Message SOAP request/response dạng generic.
import org.w3c.dom.Document; // DOM document để đọc root payload.
import org.w3c.dom.Element; // DOM element root.

import java.io.ByteArrayOutputStream; // Buffer để serialize SOAP message ra text log.
import java.nio.charset.StandardCharsets; // Charset UTF-8 khi convert bytes sang String.
import javax.xml.namespace.QName; // Tên XML gồm namespace + localPart.
import javax.xml.transform.TransformerFactory; // Transform SOAP payload source sang DOM.
import javax.xml.transform.dom.DOMResult; // Nơi nhận DOM sau transform.

public class SecureTransferOnlyInterceptor implements EndpointInterceptor { // Wrapper chỉ gọi delegate cho request secure.
    private static final Logger log = LoggerFactory.getLogger(SecureTransferOnlyInterceptor.class); // Logger để in raw SOAP XML của flow secure.

    private final EndpointInterceptor delegate; // Interceptor thật, ở đây là Wss4jSecurityInterceptor.
    private final QName payloadName; // Tên SOAP Body cần bảo vệ.

    public SecureTransferOnlyInterceptor(EndpointInterceptor delegate, String namespaceUri, String localPart) { // Constructor nhận delegate và payload cần match.
        this.delegate = delegate; // Lưu interceptor thật.
        this.payloadName = new QName(namespaceUri, localPart); // Tạo QName của SecureTransferRequest.
    }

    @Override
    public boolean handleRequest(MessageContext messageContext, Object endpoint) throws Exception { // Chạy trước endpoint.
        if (shouldVerify(messageContext)) { // Nếu request là SecureTransferRequest.
            log.info("[SOAP-WSSEC-ENDPOINT] Raw SOAP request trước khi WSS4J verify:\n{}",
                    toXml(messageContext.getRequest())); // Log nguyên SOAP Envelope request, gồm wsse:Security.
            boolean result = delegate.handleRequest(messageContext, endpoint); // Gọi WSS4J verify Timestamp + Signature.
            log.info("[SOAP-WSSEC-ENDPOINT] WSS4J verify request thành công, endpoint secure được phép xử lý");
            return result; // Trả kết quả verify cho Spring-WS.
        }
        return true; // Request khác, ví dụ SOAP custom cũ, thì bỏ qua.
    }

    @Override
    public boolean handleResponse(MessageContext messageContext, Object endpoint) throws Exception { // Chạy sau endpoint nếu response thành công.
        if (shouldVerify(messageContext)) { // Chỉ log/delegate response cho flow secure.
            boolean result = delegate.handleResponse(messageContext, endpoint); // Delegate xử lý response nếu cần.
            log.info("[SOAP-WSSEC-ENDPOINT] Raw SOAP response sau khi endpoint xử lý:\n{}",
                    toXml(messageContext.getResponse())); // Log SOAP response trả về client.
            return result; // Trả kết quả cho Spring-WS.
        }
        return true; // Flow khác thì bỏ qua.
    }

    @Override
    public boolean handleFault(MessageContext messageContext, Object endpoint) throws Exception { // Chạy khi có SOAP fault.
        if (shouldVerify(messageContext)) { // Chỉ xử lý fault cho flow secure.
            boolean result = delegate.handleFault(messageContext, endpoint); // Delegate xử lý fault nếu cần.
            log.info("[SOAP-WSSEC-ENDPOINT] Raw SOAP fault:\n{}",
                    toXml(messageContext.getResponse())); // Log SOAP fault nếu có.
            return result; // Trả kết quả cho Spring-WS.
        }
        return true; // Flow khác thì bỏ qua.
    }

    @Override
    public void afterCompletion(MessageContext messageContext, Object endpoint, Exception ex) throws Exception { // Chạy cuối request.
        if (shouldVerify(messageContext)) { // Chỉ cleanup delegate cho flow secure.
            delegate.afterCompletion(messageContext, endpoint, ex); // Delegate cleanup nếu cần.
        }
    }

    private boolean shouldVerify(MessageContext messageContext) { // Kiểm tra SOAP Body có phải SecureTransferRequest không.
        if (!(messageContext.getRequest() instanceof SoapMessage soapMessage)) { // Nếu không phải SOAP message.
            return false; // Không verify.
        }
        SoapBody body = soapMessage.getSoapBody(); // Lấy SOAP Body.
        return payloadName.equals(readPayloadName(body)); // Match namespace/localPart.
    }

    private QName readPayloadName(SoapBody body) { // Đọc QName của root element trong SOAP Body.
        try {
            DOMResult result = new DOMResult(); // Nơi chứa DOM sau khi transform.
            TransformerFactory.newInstance().newTransformer().transform(body.getPayloadSource(), result); // Chuyển payload XML sang DOM.
            Document document = (Document) result.getNode(); // Lấy DOM document.
            Element root = document.getDocumentElement(); // Root là SecureTransferRequest hoặc TransferRequest.
            return new QName(root.getNamespaceURI(), root.getLocalName()); // Trả QName của payload.
        } catch (Exception ex) {
            return null; // Nếu đọc payload lỗi thì không verify ở wrapper này.
        }
    }

    private String toXml(WebServiceMessage message) { // Serialize SOAP message thành String để log.
        if (message == null) { // Response có thể null nếu lỗi quá sớm.
            return "<null>"; // Tránh NullPointerException khi log.
        }
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(); // Buffer chứa bytes XML.
            message.writeTo(outputStream); // Ghi SOAP message ra buffer.
            return outputStream.toString(StandardCharsets.UTF_8); // Convert bytes sang UTF-8 text.
        } catch (Exception ex) {
            return "<cannot-render-soap-message: " + ex.getMessage() + ">"; // Nếu serialize lỗi thì log lý do.
        }
    }
}
