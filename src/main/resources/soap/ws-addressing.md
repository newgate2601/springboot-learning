# WS-Addressing

## 1. WS-Addressing là gì?

WS-Addressing là một chuẩn trong hệ sinh thái SOAP Web Services dùng để đưa thông tin định tuyến message vào chính SOAP Header.

Thay vì chỉ phụ thuộc vào HTTP URL, HTTP header hoặc `SOAPAction`, WS-Addressing cho phép một SOAP message tự mang các thông tin như:

- Message cần gửi tới endpoint nào.
- Message đang gọi action nào.
- Message có định danh duy nhất là gì.
- Response cần trả về đâu.
- Fault cần gửi về đâu.
- Response đang liên quan tới request nào.

Nói cách khác, WS-Addressing giúp SOAP message trở nên độc lập hơn với transport protocol như HTTP, JMS, SMTP hoặc message queue.

## 2. Vì sao cần WS-Addressing?

Trong SOAP đơn giản qua HTTP, endpoint thường được xác định bởi URL:

```http
POST /soap/account HTTP/1.1
Host: bank.example.com
SOAPAction: "http://example.com/account/GetBalance"
```

Cách này phù hợp với request-response đồng bộ thông thường.

Tuy nhiên trong hệ thống enterprise, SOAP message có thể đi qua:

- ESB.
- API Gateway.
- Message broker.
- JMS queue.
- Proxy hoặc service trung gian.
- Callback endpoint bất đồng bộ.

Khi đó, nếu chỉ dựa vào HTTP URL thì chưa đủ. Message cần tự mô tả thông tin định tuyến của nó. Đây là vai trò chính của WS-Addressing.

## 3. Namespace thường dùng

Phiên bản phổ biến của WS-Addressing dùng namespace:

```xml
xmlns:wsa="http://www.w3.org/2005/08/addressing"
```

Ví dụ khai báo trong SOAP Envelope:

```xml
<soap:Envelope
    xmlns:soap="http://www.w3.org/2003/05/soap-envelope"
    xmlns:wsa="http://www.w3.org/2005/08/addressing">
    ...
</soap:Envelope>
```

## 4. Các thành phần chính

### 4.1. `wsa:To`

`wsa:To` cho biết địa chỉ logic của service nhận message.

```xml
<wsa:To>https://bank.example.com/soap/account</wsa:To>
```

Ý nghĩa:

- Message này được gửi tới account service.
- Có thể được dùng bởi gateway, ESB hoặc SOAP framework để route message.
- Không nhất thiết phải giống hoàn toàn với HTTP URL thực tế.

### 4.2. `wsa:Action`

`wsa:Action` cho biết message đang yêu cầu thao tác nghiệp vụ nào.

```xml
<wsa:Action>http://example.com/account/GetBalance</wsa:Action>
```

Ý nghĩa:

- Tương tự khái niệm operation trong WSDL.
- Có thể dùng để map request vào method xử lý.
- Thay thế hoặc bổ sung cho HTTP header `SOAPAction`.

Ví dụ trong Spring-WS:

```java
@Endpoint
public class AccountEndpoint {

    @Action("http://example.com/account/GetBalance")
    @ResponsePayload
    public GetBalanceResponse getBalance(@RequestPayload GetBalanceRequest request) {
        return new GetBalanceResponse();
    }
}
```

Khi request có `wsa:Action` tương ứng, Spring-WS có thể route request tới method `getBalance`.

### 4.3. `wsa:MessageID`

`wsa:MessageID` là định danh duy nhất của SOAP message.

```xml
<wsa:MessageID>uuid:550e8400-e29b-41d4-a716-446655440000</wsa:MessageID>
```

Ý nghĩa:

- Dùng để trace request.
- Dùng để phát hiện duplicate message.
- Dùng để liên kết request và response.
- Rất hữu ích trong hệ thống bất đồng bộ.

### 4.4. `wsa:ReplyTo`

`wsa:ReplyTo` chỉ định nơi nhận response.

```xml
<wsa:ReplyTo>
    <wsa:Address>https://client.example.com/soap/callback</wsa:Address>
</wsa:ReplyTo>
```

Ý nghĩa:

- Service xử lý xong có thể gửi response tới endpoint này.
- Phù hợp với mô hình asynchronous request-response.
- Client không cần giữ kết nối HTTP mở trong lúc service xử lý lâu.

Nếu response trả về trên cùng kết nối hiện tại, thường dùng anonymous address:

```xml
<wsa:ReplyTo>
    <wsa:Address>http://www.w3.org/2005/08/addressing/anonymous</wsa:Address>
</wsa:ReplyTo>
```

Ý nghĩa của `anonymous`:

- Response trả về theo cơ chế transport hiện tại.
- Với HTTP, thường là trả về ngay trong HTTP response.

### 4.5. `wsa:FaultTo`

`wsa:FaultTo` chỉ định nơi nhận SOAP Fault.

```xml
<wsa:FaultTo>
    <wsa:Address>https://client.example.com/soap/fault</wsa:Address>
</wsa:FaultTo>
```

Ý nghĩa:

- Lỗi có thể được gửi tới một endpoint khác với response bình thường.
- Hữu ích khi xử lý bất đồng bộ.
- Giúp tách luồng xử lý thành công và luồng xử lý lỗi.

### 4.6. `wsa:RelatesTo`

`wsa:RelatesTo` dùng để liên kết một message với message trước đó.

Request:

```xml
<wsa:MessageID>uuid:req-001</wsa:MessageID>
```

Response:

```xml
<wsa:RelatesTo>uuid:req-001</wsa:RelatesTo>
```

Ý nghĩa:

- Response này thuộc về request có `MessageID` là `uuid:req-001`.
- Cần thiết khi nhiều request-response xử lý song song.
- Rất quan trọng trong hệ thống async hoặc queue-based.

## 5. Ví dụ SOAP request có WS-Addressing

```xml
<soap:Envelope
    xmlns:soap="http://www.w3.org/2003/05/soap-envelope"
    xmlns:wsa="http://www.w3.org/2005/08/addressing"
    xmlns:acc="http://example.com/account">

    <soap:Header>
        <wsa:To>https://bank.example.com/soap/account</wsa:To>
        <wsa:Action>http://example.com/account/GetBalance</wsa:Action>
        <wsa:MessageID>uuid:req-001</wsa:MessageID>
        <wsa:ReplyTo>
            <wsa:Address>http://www.w3.org/2005/08/addressing/anonymous</wsa:Address>
        </wsa:ReplyTo>
    </soap:Header>

    <soap:Body>
        <acc:GetBalanceRequest>
            <acc:accountNumber>123456789</acc:accountNumber>
        </acc:GetBalanceRequest>
    </soap:Body>
</soap:Envelope>
```

Giải thích:

- `wsa:To`: message gửi tới account service.
- `wsa:Action`: gọi nghiệp vụ `GetBalance`.
- `wsa:MessageID`: định danh duy nhất của request.
- `wsa:ReplyTo`: response trả về trên cùng transport hiện tại.
- `soap:Body`: chứa dữ liệu nghiệp vụ thật sự.

## 6. Ví dụ SOAP response có WS-Addressing

```xml
<soap:Envelope
    xmlns:soap="http://www.w3.org/2003/05/soap-envelope"
    xmlns:wsa="http://www.w3.org/2005/08/addressing"
    xmlns:acc="http://example.com/account">

    <soap:Header>
        <wsa:Action>http://example.com/account/GetBalanceResponse</wsa:Action>
        <wsa:MessageID>uuid:res-001</wsa:MessageID>
        <wsa:RelatesTo>uuid:req-001</wsa:RelatesTo>
    </soap:Header>

    <soap:Body>
        <acc:GetBalanceResponse>
            <acc:accountNumber>123456789</acc:accountNumber>
            <acc:balance>1000000</acc:balance>
            <acc:currency>VND</acc:currency>
        </acc:GetBalanceResponse>
    </soap:Body>
</soap:Envelope>
```

Giải thích:

- `wsa:Action`: đây là response của nghiệp vụ `GetBalance`.
- `wsa:MessageID`: định danh riêng của response.
- `wsa:RelatesTo`: response này liên quan tới request `uuid:req-001`.

## 7. WS-Addressing và SOAPAction

`SOAPAction` là HTTP header, thường gặp trong SOAP 1.1:

```http
SOAPAction: "http://example.com/account/GetBalance"
```

`wsa:Action` nằm trong SOAP Header:

```xml
<wsa:Action>http://example.com/account/GetBalance</wsa:Action>
```

So sánh:

| Tiêu chí | SOAPAction | WS-Addressing Action |
| --- | --- | --- |
| Vị trí | HTTP Header | SOAP Header |
| Phụ thuộc HTTP | Có | Không |
| Dùng tốt với JMS/MQ | Hạn chế | Có |
| Hỗ trợ routing phức tạp | Hạn chế | Tốt hơn |
| Hỗ trợ async callback | Không trực tiếp | Có |
| Gắn với chuẩn WS-* | Không đầy đủ | Có |

Trong hệ thống đơn giản, `SOAPAction` có thể đủ. Trong hệ thống enterprise, `wsa:Action` thường linh hoạt hơn vì nó đi cùng SOAP message.

## 8. Luồng xử lý đồng bộ

Với HTTP request-response thông thường:

1. Client gửi SOAP request tới service.
2. SOAP Header có `wsa:To`, `wsa:Action`, `wsa:MessageID`.
3. `wsa:ReplyTo` dùng anonymous address.
4. Service xử lý request.
5. Service trả SOAP response ngay trên HTTP response.
6. Response có thể chứa `wsa:RelatesTo` trỏ về `MessageID` của request.

Luồng này vẫn giống HTTP synchronous thông thường, nhưng message có thêm metadata chuẩn hóa.

## 9. Luồng xử lý bất đồng bộ

Với asynchronous callback:

1. Client gửi SOAP request tới service.
2. Request có `wsa:MessageID`.
3. Request có `wsa:ReplyTo` là callback endpoint của client.
4. Service nhận request và trả acknowledgement hoặc không trả kết quả ngay.
5. Sau khi xử lý xong, service gửi SOAP response tới địa chỉ trong `wsa:ReplyTo`.
6. Response có `wsa:RelatesTo` trỏ về `MessageID` ban đầu.

Mô hình này phù hợp với nghiệp vụ xử lý lâu, ví dụ:

- Thanh toán.
- Đối soát giao dịch.
- Phê duyệt hồ sơ.
- Đồng bộ dữ liệu giữa các hệ thống.
- Gửi request qua queue.

## 10. Khi nào nên dùng WS-Addressing?

Nên dùng khi:

- SOAP message đi qua nhiều tầng trung gian.
- Cần routing dựa trên SOAP Header.
- Cần correlation giữa request và response.
- Cần callback endpoint.
- Cần xử lý bất đồng bộ.
- Cần tích hợp với ESB hoặc message broker.
- Cần dùng chung với các chuẩn WS-* như WS-Security, WS-ReliableMessaging hoặc WS-Policy.

Không nhất thiết cần khi:

- Service SOAP chỉ chạy HTTP đồng bộ đơn giản.
- Endpoint cố định.
- Không có callback.
- Không có queue hoặc ESB.
- Operation đã được xác định rõ bằng payload mapping.

## 11. WS-Addressing trong Spring-WS

Spring Web Services có thể map endpoint dựa trên WS-Addressing Action.

Ví dụ:

```java
@Endpoint
public class PaymentEndpoint {

    @Action("http://example.com/payment/CreatePayment")
    @ResponsePayload
    public CreatePaymentResponse createPayment(@RequestPayload CreatePaymentRequest request) {
        return new CreatePaymentResponse();
    }
}
```

Request tương ứng:

```xml
<soap:Header>
    <wsa:Action>http://example.com/payment/CreatePayment</wsa:Action>
</soap:Header>
```

Khi cấu hình endpoint mapping phù hợp, Spring-WS có thể dùng `wsa:Action` để chọn method xử lý.

## 12. Quan hệ với WSDL

Trong WSDL, operation có thể được gắn với một action cụ thể.

Ví dụ ý tưởng:

```xml
<wsdl:operation name="GetBalance">
    <soap:operation soapAction="http://example.com/account/GetBalance"/>
</wsdl:operation>
```

Với WS-Addressing, action có thể được biểu diễn rõ hơn như metadata của message. Client sinh code từ WSDL thường có thể tự thêm action tương ứng vào SOAP Header hoặc HTTP header, tùy framework.

## 13. Lỗi thường gặp

### 13.1. Thiếu `wsa:Action`

Service có thể không biết request cần map vào operation nào nếu endpoint mapping dựa trên action.

### 13.2. Sai namespace WS-Addressing

Ví dụ dùng nhầm namespace:

```xml
http://schemas.xmlsoap.org/ws/2004/08/addressing
```

thay vì:

```xml
http://www.w3.org/2005/08/addressing
```

Một số hệ thống cũ dùng namespace cũ, nên client và server phải thống nhất phiên bản.

### 13.3. `MessageID` không duy nhất

Nếu nhiều message dùng cùng `MessageID`, hệ thống correlation hoặc duplicate detection có thể xử lý sai.

### 13.4. `ReplyTo` không truy cập được

Trong async callback, service phải gọi được endpoint trong `ReplyTo`. Nếu endpoint nằm sau firewall hoặc không public, callback sẽ thất bại.

### 13.5. `RelatesTo` sai

Nếu response trỏ sai `MessageID`, client có thể không ghép được response với request ban đầu.

## 14. Tóm tắt

WS-Addressing là chuẩn đưa thông tin địa chỉ và correlation vào SOAP Header.

Các header quan trọng:

| Header | Ý nghĩa |
| --- | --- |
| `wsa:To` | Địa chỉ logic của service nhận message |
| `wsa:Action` | Hành động hoặc operation cần gọi |
| `wsa:MessageID` | Định danh duy nhất của message |
| `wsa:ReplyTo` | Nơi nhận response |
| `wsa:FaultTo` | Nơi nhận SOAP Fault |
| `wsa:RelatesTo` | Message hiện tại liên quan tới message nào |

Nếu SOAP service chỉ là HTTP request-response đơn giản, WS-Addressing có thể không bắt buộc. Nhưng với hệ thống enterprise có routing, async callback, queue, ESB hoặc correlation phức tạp, WS-Addressing là một chuẩn rất quan trọng.
