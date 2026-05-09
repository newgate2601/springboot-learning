# SOAP Concepts

## 1. SOAP là gì?

SOAP, viết tắt của Simple Object Access Protocol, là một giao thức trao đổi message giữa các hệ thống. SOAP thường được dùng để tích hợp service trong môi trường enterprise, đặc biệt là các hệ thống ngân hàng, bảo hiểm, chính phủ, ERP, CRM và các hệ thống legacy.

SOAP message được đóng gói bằng XML. Một message có cấu trúc chính:

```xml
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
  <soap:Header>
    <!-- metadata: security, routing, transaction... -->
  </soap:Header>
  <soap:Body>
    <GetCustomerRequest>
      <CustomerId>123</CustomerId>
    </GetCustomerRequest>
  </soap:Body>
</soap:Envelope>
```

Thành phần chính:

- `Envelope`: lớp bao ngoài cho biết đây là SOAP message.
- `Header`: chứa metadata như security token, signature, transaction, routing.
- `Body`: chứa dữ liệu nghiệp vụ.
- `Fault`: cấu trúc lỗi chuẩn của SOAP.

## 2. WSDL và XSD

SOAP thường đi kèm WSDL. WSDL là contract mô tả service:

- service có những operation nào
- endpoint URL là gì
- request/response gồm field nào
- type của field là gì
- namespace nào được dùng
- binding và transport nào được dùng
- operation có thể trả những fault nào

XSD được dùng để mô tả schema của XML message. Ví dụ:

```xml
<xsd:complexType name="TransferRequest">
  <xsd:sequence>
    <xsd:element name="fromAccount" type="xsd:string"/>
    <xsd:element name="toAccount" type="xsd:string"/>
    <xsd:element name="amount" type="xsd:decimal"/>
  </xsd:sequence>
</xsd:complexType>
```

Với schema trên, SOAP server có thể validate request:

- có đủ field bắt buộc không
- field có đúng thứ tự không
- type có đúng không
- enum, pattern, min, max có hợp lệ không
- namespace có khớp không

## 3. Stub và Proxy

Stub/proxy là code client được generate từ WSDL. Thay vì tự viết XML SOAP request, developer gọi method như code bình thường:

```java
TransferResponse response = paymentService.transfer(request);
```

Bên dưới, stub/proxy sẽ:

1. Convert object Java/C# thành XML SOAP request.
2. Gửi request đến SOAP endpoint.
3. Nhận XML SOAP response.
4. Convert XML response thành object.
5. Ném exception nếu server trả SOAP Fault.

Nếu WSDL và XSD chuẩn, code generator có thể tự xử lý phần lớn các chi tiết như field order, namespace, type, SOAP action và operation mapping.

Tuy nhiên, stub/proxy không có nghĩa là tích hợp hoàn toàn tự động. Các quy tắc security, certificate, endpoint test/prod, custom header, retry, idempotency và rule nghiệp vụ vẫn thường nằm trong integration guide riêng.

## 4. Namespace

Namespace trong XML dùng để tránh trùng tên element.

```xml
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"
               xmlns:pay="http://example.com/payment">
  <soap:Body>
    <pay:TransferRequest>
      <pay:Amount>1000000</pay:Amount>
    </pay:TransferRequest>
  </soap:Body>
</soap:Envelope>
```

`soap:Body` và `pay:Body` là hai element khác nhau nếu namespace khác nhau, dù cùng tên `Body`.

SOAP rất nhạy với namespace. Nhiều lỗi tích hợp xảy ra không phải vì thiếu field, mà vì element nằm sai namespace.

## 5. Binding và Transport

Binding mô tả cách operation được đóng gói thành SOAP message.

Transport là kênh truyền message.

Ví dụ WSDL binding:

```xml
<soap:binding style="document"
              transport="http://schemas.xmlsoap.org/soap/http"/>
```

Ý nghĩa:

- `style="document"`: SOAP body chứa XML document theo schema.
- `transport="...soap/http"`: SOAP message được gửi qua HTTP.

SOAP thường chạy qua HTTP/HTTPS, nhưng về lý thuyết có thể chạy qua SMTP, JMS, TCP hoặc message broker.

## 6. Fault Schema

SOAP Fault là format lỗi chuẩn của SOAP.

```xml
<soap:Fault>
  <faultcode>soap:Client</faultcode>
  <faultstring>Invalid account</faultstring>
  <detail>
    <InvalidAccountFault>
      <code>ACCOUNT_NOT_FOUND</code>
      <message>Account does not exist</message>
    </InvalidAccountFault>
  </detail>
</soap:Fault>
```

Fault schema trong WSDL/XSD giúp client biết operation có thể trả những lỗi nào. Tool generate code có thể tạo exception typed, ví dụ:

```java
try {
    paymentService.transfer(request);
} catch (InsufficientBalanceFault e) {
    // xử lý thiếu số dư
} catch (InvalidAccountFault e) {
    // xử lý tài khoản sai
}
```

## 7. Strong Typing

SOAP được xem là strong typing vì message được mô tả bằng XSD. Field có kiểu rõ ràng:

- `xsd:string`
- `xsd:int`
- `xsd:decimal`
- `xsd:dateTime`
- enum
- complex type

Nếu schema yêu cầu:

```xml
<amount>1000000</amount>
```

mà client gửi:

```xml
<amount>abc</amount>
```

server có thể reject vì `abc` không phải decimal.

REST cũng có thể strong typing bằng OpenAPI và JSON Schema, nhưng SOAP/WSDL/XSD có lịch sử tooling contract-first rất mạnh trong Java/.NET enterprise.

## 8. XML Verbose

SOAP dùng XML nên payload dài và nặng hơn JSON.

SOAP/XML:

```xml
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"
               xmlns:cus="http://example.com/customer">
  <soap:Body>
    <cus:GetCustomerResponse>
      <cus:Customer>
        <cus:Id>123</cus:Id>
        <cus:Name>Nguyen Van A</cus:Name>
        <cus:Status>ACTIVE</cus:Status>
      </cus:Customer>
    </cus:GetCustomerResponse>
  </soap:Body>
</soap:Envelope>
```

REST/JSON:

```json
{
  "id": "123",
  "name": "Nguyen Van A",
  "status": "ACTIVE"
}
```

XML verbose làm SOAP:

- tốn bandwidth hơn
- parse chậm hơn JSON/Protobuf
- log dài hơn
- debug bằng tay khó hơn
- kém thân thiện với frontend/mobile

## 9. SOAP nghiêm ngặt ở điểm nào?

"Nghiêm ngặt" không có nghĩa REST không làm được. REST có thể rất nghiêm nếu dùng OpenAPI, JSON Schema, gateway validation, mTLS, OAuth2, request signature và contract testing.

SOAP được xem là nghiêm ngặt hơn trong enterprise vì nó ép nhiều thứ vào contract:

- operation rõ ràng
- request/response schema rõ ràng
- field order rõ ràng qua `xsd:sequence`
- namespace rõ ràng
- type rõ ràng
- fault rõ ràng
- tooling generate client/server từ WSDL

Với các hệ thống như banking, insurance hoặc government, contract thay đổi chậm, được review kỹ, và client/server có thể được generate từ contract đó. Đây là lý do SOAP từng rất phù hợp với các tích hợp liên tổ chức.

## 10. Yếu tố lịch sử: vì sao SOAP từng dùng nhiều?

SOAP phổ biến mạnh trong giai đoạn REST/JSON/OpenAPI/OAuth2 chưa trưởng thành như hiện nay.

Ở thời điểm đó, enterprise cần:

- contract chính thức giữa các tổ chức
- client/server code generation cho Java và .NET
- XML Schema validation
- typed fault
- tích hợp với ESB
- security, transaction, reliable messaging theo các chuẩn WS-*
- vendor tooling từ IBM, Oracle, Microsoft, SAP

SOAP/WSDL lúc đó là một lựa chọn có tính "standard" cao hơn so với việc mỗi tổ chức tự định nghĩa HTTP API riêng.

REST về sau trở nên phổ biến hơn vì:

- JSON nhẹ và dễ đọc hơn XML
- HTTP API dễ debug hơn
- OpenAPI thay thế một phần vai trò WSDL
- OAuth2/OIDC/JWT trở nên phổ biến
- API Gateway và cloud ecosystem mạnh
- frontend/mobile dùng REST dễ hơn
- developer experience tốt hơn

Vì vậy, trong hệ thống mới, REST/gRPC thường thay được SOAP. SOAP còn tồn tại chủ yếu vì legacy, vendor, compliance, quy trình enterprise cũ, hoặc vì đối tác đã expose WSDL.

## 11. Khi nào nên chọn SOAP?

Nên chọn SOAP khi:

- đối tác chỉ cung cấp WSDL/SOAP
- hệ thống legacy bắt buộc SOAP
- cần WS-Security/WS-* interoperability
- doanh nghiệp đã có ESB/SOAP stack
- contract liên tổ chức đã chuẩn hóa quanh WSDL/XSD
- vendor chỉ certify SOAP integration

Không nên mặc định chọn SOAP cho API mới nếu không có ràng buộc trên. Với greenfield system, REST hoặc gRPC thường hợp lý hơn.

