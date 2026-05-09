package com.example.learning.integration; // Package chứa code giả lập bên tích hợp.

import org.apache.cxf.binding.soap.SoapMessage; // Message SOAP của Apache CXF.
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor; // Base class để viết interceptor cho SOAP message.
import org.apache.cxf.headers.Header; // Object header của CXF.
import org.apache.cxf.phase.Phase; // Phase xác định thời điểm interceptor chạy.
import org.w3c.dom.Document; // DOM document dùng để tạo XML header.
import org.w3c.dom.Element; // DOM element dùng để tạo từng tag XML.

import javax.xml.namespace.QName; // Tên XML gồm namespace + local name.
import javax.xml.parsers.DocumentBuilderFactory; // Factory tạo DOM document.

public class PartnerSecurityHeaderOutInterceptor extends AbstractSoapInterceptor { // Interceptor thêm PartnerSecurity vào SOAP Header.
    private static final String SECURITY_NAMESPACE = "http://example.com/learning/security"; // Namespace của SOAP Header security demo.

    private final String clientId; // clientId sẽ được ghi vào <sec:clientId>.
    private final String timestamp; // timestamp sẽ được ghi vào <sec:timestamp>.
    private final String signature; // signature sẽ được ghi vào <sec:signature>.

    public PartnerSecurityHeaderOutInterceptor(String clientId, String timestamp, String signature) { // Constructor nhận dữ liệu header.
        super(Phase.PREPARE_SEND); // Chạy trước khi CXF gửi SOAP message ra ngoài.
        this.clientId = clientId; // Lưu clientId.
        this.timestamp = timestamp; // Lưu timestamp.
        this.signature = signature; // Lưu signature.
    }

    @Override // Ghi đè method xử lý message của CXF interceptor.
    public void handleMessage(SoapMessage message) { // Được gọi khi SOAP request chuẩn bị gửi đi.
        message.getHeaders().add(new Header( // Thêm một header mới vào SOAP message.
                new QName(SECURITY_NAMESPACE, "PartnerSecurity"), // Tên header là PartnerSecurity thuộc namespace security.
                createSecurityHeaderElement() // Nội dung header là DOM element mình tự tạo.
        ));
    }

    private Element createSecurityHeaderElement() { // Tạo XML <sec:PartnerSecurity>...</sec:PartnerSecurity>.
        try {
            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument(); // Tạo DOM document rỗng.
            Element root = document.createElementNS(SECURITY_NAMESPACE, "sec:PartnerSecurity"); // Tạo root header có namespace.
            root.appendChild(child(document, "clientId", clientId)); // Thêm <sec:clientId>.
            root.appendChild(child(document, "timestamp", timestamp)); // Thêm <sec:timestamp>.
            root.appendChild(child(document, "signature", signature)); // Thêm <sec:signature>.
            return root; // Trả root element để CXF gắn vào SOAP Header.
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot create SOAP security header", ex); // Báo lỗi nếu tạo XML thất bại.
        }
    }

    private Element child(Document document, String localName, String value) { // Helper tạo một child element.
        Element element = document.createElementNS(SECURITY_NAMESPACE, "sec:" + localName); // Tạo tag con có cùng namespace security.
        element.setTextContent(value); // Gán text bên trong tag.
        return element; // Trả element cho root append.
    }
}
