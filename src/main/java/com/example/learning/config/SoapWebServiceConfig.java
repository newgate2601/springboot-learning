package com.example.learning.config;

import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.ws.config.annotation.EnableWs;
import org.springframework.ws.config.annotation.WsConfigurerAdapter;
import org.springframework.ws.transport.http.MessageDispatcherServlet;
import org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition;
import org.springframework.xml.xsd.SimpleXsdSchema;
import org.springframework.xml.xsd.XsdSchema;

@EnableWs // Bật Spring Web Services để app có thể nhận SOAP request.
@Configuration // Báo Spring đây là class cấu hình.
public class SoapWebServiceConfig extends WsConfigurerAdapter {
    public static final String TRANSFER_NAMESPACE = "http://example.com/learning/transfer"; // Namespace chính của transfer SOAP service.
    public static final String SECURE_TRANSFER_NAMESPACE = "http://example.com/learning/secure-transfer"; // Namespace riêng cho flow WS-Security chuẩn.

    @Bean // Đăng ký servlet chuyên xử lý SOAP request.
    public ServletRegistrationBean<MessageDispatcherServlet> messageDispatcherServlet(ApplicationContext context) {
        MessageDispatcherServlet servlet = new MessageDispatcherServlet(); // Dispatcher riêng của Spring-WS, tương tự DispatcherServlet cho REST.
        servlet.setApplicationContext(context); // Cho servlet biết Spring context để tìm @Endpoint.
        servlet.setTransformWsdlLocations(true); // WSDL sẽ tự render location theo host/port hiện tại.
        return new ServletRegistrationBean<>(servlet, "/ws/*"); // Tất cả request /ws/* sẽ đi vào SOAP servlet.
    }

    @Bean(name = "transfers") // Tên bean quyết định URL WSDL: /ws/transfers.wsdl.
    public DefaultWsdl11Definition transferWsdl(XsdSchema transferSchema) {
        DefaultWsdl11Definition definition = new DefaultWsdl11Definition(); // Spring sẽ sinh WSDL từ XSD.
        definition.setPortTypeName("TransferPort"); // Tên portType trong WSDL, client generate code sẽ thấy tên này.
        definition.setLocationUri("/ws"); // Endpoint SOAP thật sự để client call.
        definition.setTargetNamespace(TRANSFER_NAMESPACE); // Namespace của WSDL/service.
        definition.setSchema(transferSchema); // Gắn XSD contract vào WSDL.
        return definition;
    }

    @Bean // Load XSD contract từ resources.
    public XsdSchema transferSchema() {
        return new SimpleXsdSchema(new ClassPathResource("ws/transfer.xsd")); // File định nghĩa request/response SOAP.
    }

    @Bean(name = "secure-transfers") // WSDL runtime: /ws/secure-transfers.wsdl.
    public DefaultWsdl11Definition secureTransferWsdl(XsdSchema secureTransferSchema) {
        DefaultWsdl11Definition definition = new DefaultWsdl11Definition(); // Sinh WSDL động từ XSD secure.
        definition.setPortTypeName("SecureTransferPort"); // Tên portType của SOAP service secure.
        definition.setLocationUri("/ws"); // Endpoint SOAP vẫn là /ws, phân biệt bằng namespace/localPart.
        definition.setTargetNamespace(SECURE_TRANSFER_NAMESPACE); // Namespace riêng của flow secure.
        definition.setSchema(secureTransferSchema); // Gắn XSD secure vào WSDL.
        return definition;
    }

    @Bean
    public XsdSchema secureTransferSchema() {
        return new SimpleXsdSchema(new ClassPathResource("ws/secure-transfer.xsd")); // Contract cho endpoint dùng WS-Security.
    }
}
