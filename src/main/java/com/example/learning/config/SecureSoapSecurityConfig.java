package com.example.learning.config; // Package cấu hình Spring.

import org.springframework.context.annotation.Bean; // Annotation khai báo bean.
import org.springframework.context.annotation.Configuration; // Annotation khai báo class cấu hình.
import org.springframework.core.io.ClassPathResource; // Load file trong src/main/resources.
import org.springframework.ws.config.annotation.WsConfigurerAdapter; // Base class để cấu hình Spring-WS.
import org.springframework.ws.server.EndpointInterceptor; // Interceptor của Spring-WS.
import org.springframework.ws.soap.security.wss4j2.Wss4jSecurityInterceptor; // Interceptor WS-Security dùng WSS4J để verify.
import org.springframework.ws.soap.security.wss4j2.support.CryptoFactoryBean; // Factory tạo crypto config từ truststore/keystore.

import java.util.List; // Danh sách interceptor.

@Configuration // Cấu hình WS-Security cho endpoint secure.
public class SecureSoapSecurityConfig extends WsConfigurerAdapter {
    private static final String SECURE_TRANSFER_NAMESPACE = "http://example.com/learning/secure-transfer"; // Namespace endpoint secure.

    @Override
    public void addInterceptors(List<EndpointInterceptor> interceptors) { // Spring-WS gọi method này để đăng ký interceptor.
        try {
            interceptors.add(new SecureTransferOnlyInterceptor( // Chỉ áp dụng security cho payload secure, không đụng flow SOAP custom cũ.
                    secureTransferWss4jSecurityInterceptor(), // WSS4J interceptor sẽ verify Timestamp + Signature.
                    SECURE_TRANSFER_NAMESPACE, // Namespace của SOAP Body secure.
                    "SecureTransferRequest" // Local name của request secure.
            ));
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot configure WS-Security interceptor", ex); // Fail fast nếu truststore cấu hình sai.
        }
    }

    @Bean
    public Wss4jSecurityInterceptor secureTransferWss4jSecurityInterceptor() throws Exception { // Bean verify WS-Security phía server.
        Wss4jSecurityInterceptor interceptor = new Wss4jSecurityInterceptor(); // Tạo interceptor WSS4J.
        interceptor.setValidationActions("Timestamp Signature"); // Bắt buộc request có Timestamp và Signature.
        interceptor.setValidationSignatureCrypto(secureTransferValidationCrypto().getObject()); // Dùng truststore để verify certificate/signature.
        return interceptor; // Trả interceptor cho Spring-WS.
    }

    @Bean
    public CryptoFactoryBean secureTransferValidationCrypto() throws Exception { // Crypto config phía server.
        CryptoFactoryBean cryptoFactoryBean = new CryptoFactoryBean(); // Factory của Spring-WS.
        cryptoFactoryBean.setKeyStoreLocation(new ClassPathResource("security/server-truststore.jks")); // Truststore chứa public cert của partner.
        cryptoFactoryBean.setKeyStorePassword("changeit"); // Password demo của truststore.
        return cryptoFactoryBean; // WSS4J dùng truststore này để verify chữ ký.
    }
}
