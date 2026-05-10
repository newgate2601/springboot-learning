# SOAP WSDL Integration Guide

Tài liệu này giải thích hai phía:

- Phía hệ thống của mình: expose SOAP endpoint và WSDL.
- Phía bên tích hợp: lấy WSDL để generate client code rồi gọi SOAP service.

## 1. Nếu chỉ muốn tạo SOAP endpoint trong hệ thống của mình

Các file/công cụ cần có:

| Thành phần | File trong project | Vai trò |
|---|---|---|
| Dependency SOAP server | `pom.xml` | Thêm `spring-boot-starter-web-services` để Spring Boot nhận SOAP request. |
| Dependency WSDL runtime | `pom.xml` | Thêm `wsdl4j` để Spring-WS sinh WSDL động bằng `DefaultWsdl11Definition`. |
| SOAP config | `src/main/java/com/example/learning/config/SoapWebServiceConfig.java` | Map `/ws/*`, expose `/ws/transfers.wsdl`, load XSD. |
| XSD contract | `src/main/resources/ws/transfer.xsd` | Định nghĩa `TransferRequest` và `TransferResponse`. |
| SOAP request DTO | `src/main/java/com/example/learning/soap/SoapTransferRequest.java` | Java object để Spring map SOAP Body XML thành object. |
| SOAP response DTO | `src/main/java/com/example/learning/soap/SoapTransferResponse.java` | Java object để Spring map object thành SOAP response XML. |
| SOAP endpoint | `src/main/java/com/example/learning/controller/TransferSoapEndpoint.java` | Nhận SOAP request, đọc SOAP Header, gọi service nghiệp vụ. |
| Business service | `src/main/java/com/example/learning/transfer/PartnerTransferService.java` | Xử lý nghiệp vụ thật, không phụ thuộc SOAP hay REST. |
| Security service | `src/main/java/com/example/learning/security/PartnerSecurityService.java` | Verify clientId, timestamp, signature. |

Luồng server:

```text
POST /ws
  -> MessageDispatcherServlet
  -> TransferSoapEndpoint
  -> PartnerSecurityService
  -> PartnerTransferService
  -> SOAP XML response
```

WSDL động:

```text
GET http://localhost:8086/ws/transfers.wsdl
```

Cơ chế sinh WSDL:

```text
transfer.xsd
  -> XsdSchema bean
  -> DefaultWsdl11Definition bean tên "transfers"
  -> /ws/transfers.wsdl
```

## 2. Nếu muốn bên tích hợp generate code từ WSDL

Bên tích hợp cần:

| Thành phần | Ý nghĩa |
|---|---|
| WSDL URL/file | Contract để tool generate client code. Ví dụ `http://localhost:8086/ws/transfers.wsdl`. |
| Endpoint URL | URL gọi thật, ví dụ `http://localhost:8086/ws`. |
| Security document | Quy định SOAP Header, clientId, timestamp, signature. |
| Credential test | Ví dụ `clientId=partner-mobile`, `secret=demo-partner-secret`. |
| Tool generate code | Java có thể dùng Apache CXF `wsdl2java`, Maven `cxf-codegen-plugin`, hoặc `wsimport` nếu dùng stack cũ. |

Trong project này, mình demo ngay trong cùng repo bằng Apache CXF:

```xml
<plugin>
    <groupId>org.apache.cxf</groupId>
    <artifactId>cxf-codegen-plugin</artifactId>
    <version>${cxf.version}</version>
</plugin>
```

File WSDL tĩnh dùng để generate:

```text
src/main/resources/ws/transfers.wsdl
```

Generated source được sinh vào:

```text
target/generated-sources/cxf/com/example/learning/generated/transfer/
```

Các class được generate:

```text
TransferPort
TransferPortService
TransferRequest
TransferResponse
ObjectFactory
```

Quan trọng: các file trong `target/generated-sources/cxf` không nên sửa tay, vì Maven có thể sinh lại và ghi đè.

## 3. Code bên tích hợp hoạt động thế nào

File đại diện bên tích hợp:

```text
src/main/java/com/example/learning/controller/PartnerIntegrationController.java
```

API test:

```text
POST http://localhost:8086/api/v1/integrator/transfer-via-soap
```

Controller này nhận JSON để dễ test bằng Postman, rồi map sang class generated:

```java
TransferRequest soapRequest = new TransferRequest();
```

`TransferRequest` ở đây là class được generate từ WSDL, không phải DTO tự viết.

Client gọi SOAP:

```text
src/main/java/com/example/learning/integration/PartnerTransferSoapClient.java
```

Điểm quan trọng:

```java
TransferPortService service = new TransferPortService();
TransferPort port = service.getTransferPortSoap11();
TransferResponse response = port.transfer(request);
```

`port.transfer(request)` là method được generate từ operation `Transfer` trong WSDL.

## 4. Vì sao vẫn cần map từ JSON sang TransferRequest?

Trong demo này có thêm REST API giả lập partner để bạn dễ gọi bằng Postman:

```text
Postman JSON -> REST API giả lập partner -> generated SOAP client -> SOAP server
```

Vì đầu vào của API giả lập là JSON, nên vẫn cần map:

```text
TransferRestRequest -> TransferRequest generated từ WSDL
```

Trong thực tế, nếu bên tích hợp là hệ thống backend gọi SOAP trực tiếp, họ không cần REST API trung gian này. Họ sẽ tạo thẳng `TransferRequest` generated và gọi:

```java
TransferResponse response = port.transfer(request);
```

## 5. Security Header

WSDL hiện tại mô tả SOAP Body, chưa mô tả security header.

Vì vậy bên tích hợp vẫn cần tài liệu security riêng:

```text
SOAP Header namespace: http://example.com/learning/security
Header name: PartnerSecurity
Fields: clientId, timestamp, signature
```

Trong project demo, file gắn header là:

```text
src/main/java/com/example/learning/integration/PartnerSecurityHeaderOutInterceptor.java
```

Header được gửi lên server:

```xml
<sec:PartnerSecurity xmlns:sec="http://example.com/learning/security">
    <sec:clientId>partner-mobile</sec:clientId>
    <sec:timestamp>2125-05-09T00:00:00Z</sec:timestamp>
    <sec:signature>...</sec:signature>
</sec:PartnerSecurity>
```

## 6. Tóm tắt

Phía mình expose SOAP:

```text
pom.xml
SoapWebServiceConfig.java
transfer.xsd
SoapTransferRequest.java
SoapTransferResponse.java
TransferSoapEndpoint.java
Business service
Security service
```

Phía tích hợp consume SOAP:

```text
WSDL
CXF wsdl2java hoặc tool tương đương
Generated classes
Endpoint URL
Security rule
Credential
```

Điểm cốt lõi:

```text
Server dùng XSD/WSDL để công bố contract.
Partner dùng WSDL để generate client code.
Generated code giúp partner gọi method Java thay vì tự viết SOAP XML.
```

## 7. Flow mới: WS-Security chuẩn, không tự ký HMAC thủ công

Mình giữ nguyên flow cũ để so sánh:

```text
Custom SOAP Header:
PartnerSecurityHeaderOutInterceptor
PartnerSecurityService.sign(...)
TransferSoapEndpoint tự đọc header
```

Flow mới dùng WS-Security chuẩn:

```text
CXF generated client
  -> WSS4JOutInterceptor tự thêm wsse:Security
  -> SOAP request có Timestamp + XML Signature
  -> Spring-WS Wss4jSecurityInterceptor verify chữ ký
  -> SecureTransferSoapEndpoint xử lý nghiệp vụ
```

Các file server expose SOAP secure:

| File | Vai trò |
|---|---|
| `src/main/resources/ws/secure-transfer.xsd` | Contract XSD riêng cho endpoint WS-Security. |
| `src/main/resources/ws/secure-transfers.wsdl` | WSDL tĩnh để CXF generate client code. |
| `src/main/java/com/example/learning/config/SecureSoapSecurityConfig.java` | Đăng ký WSS4J verify Timestamp + Signature. |
| `src/main/java/com/example/learning/config/SecureTransferOnlyInterceptor.java` | Chỉ áp dụng WSS4J cho `SecureTransferRequest`, không ảnh hưởng flow cũ. |
| `src/main/java/com/example/learning/controller/SecureTransferSoapEndpoint.java` | Endpoint SOAP secure, không tự parse signature. |
| `src/main/java/com/example/learning/soap/secure/SecureSoapTransferRequest.java` | DTO server nhận SOAP Body secure. |
| `src/main/java/com/example/learning/soap/secure/SecureSoapTransferResponse.java` | DTO server trả SOAP Body secure. |

Các file phía partner tích hợp:

| File | Vai trò |
|---|---|
| `src/main/java/com/example/learning/controller/PartnerWsSecurityIntegrationController.java` | REST API giả lập partner để test dễ bằng Postman/curl. |
| `src/main/java/com/example/learning/integration/wssec/PartnerWsSecuritySoapClient.java` | Dùng generated client từ WSDL và gắn `WSS4JOutInterceptor`. |
| `src/main/java/com/example/learning/integration/wssec/PartnerKeystorePasswordCallback.java` | Trả password private key cho WSS4J khi ký. |
| `src/main/resources/security/partner-wss4j.properties` | Cấu hình keystore để client ký message. |
| `src/main/resources/security/partner-keystore.jks` | Keystore demo chứa private key của partner. |
| `src/main/resources/security/server-truststore.jks` | Truststore demo server dùng để verify certificate của partner. |

API test flow WS-Security:

```text
POST http://localhost:8086/api/v1/integrator-wssec/transfer-via-soap
```

Script test:

```powershell
.\tools\call-integrator-wssec-transfer.ps1
```

Điểm khác biệt lớn:

```text
Flow cũ: dev tự tạo sec:PartnerSecurity và tự verify HMAC.
Flow mới: framework tự tạo wsse:Security, tự ký XML Signature, server tự verify bằng WSS4J.
```

Nhưng bên tích hợp vẫn phải cấu hình:

```text
private key
certificate
keystore password
alias
WSS4JOutInterceptor
```

WSDL giúp generate request/response/port/service. Phần security chuẩn có thể được mô tả thêm bằng WS-SecurityPolicy, nhưng thực tế vẫn thường cần tài liệu hướng dẫn keystore/certificate riêng.
