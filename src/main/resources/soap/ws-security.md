# WS-Security

## 1. WS-Security là gì?

WS-Security là một chuẩn mở rộng cho SOAP, dùng để đưa thông tin bảo mật vào `SOAP Header`.

Nó cho phép SOAP message mang theo:

- token xác thực
- chữ ký số
- mã hóa message
- timestamp
- nonce
- certificate
- reference đến phần message được ký hoặc mã hóa

Ví dụ minh họa:

```xml
<soap:Envelope>
  <soap:Header>
    <wsse:Security>
      <wsu:Timestamp>
        <wsu:Created>2026-05-09T10:00:00Z</wsu:Created>
        <wsu:Expires>2026-05-09T10:05:00Z</wsu:Expires>
      </wsu:Timestamp>

      <wsse:BinarySecurityToken>
        <!-- X.509 certificate -->
      </wsse:BinarySecurityToken>

      <ds:Signature>
        <!-- digital signature over SOAP Body / headers -->
      </ds:Signature>
    </wsse:Security>
  </soap:Header>

  <soap:Body>
    <TransferMoneyRequest>
      <FromAccount>111</FromAccount>
      <ToAccount>222</ToAccount>
      <Amount>1000000</Amount>
    </TransferMoneyRequest>
  </soap:Body>
</soap:Envelope>
```

Trong thực tế, phần signature và encryption rất dài vì WS-Security dựa trên XML Signature và XML Encryption.

## 2. HTTPS bảo vệ gì?

HTTPS/TLS bảo vệ kênh truyền.

Nó giúp:

- mã hóa dữ liệu trên đường truyền
- xác thực server certificate
- tránh nghe lén network
- tránh sửa dữ liệu khi đang truyền trên một connection

Nhưng HTTPS chủ yếu bảo vệ từng chặng transport:

```text
Client -> Gateway: TLS
Gateway -> ESB: TLS
ESB -> Service: TLS
```

Tại mỗi điểm trung gian terminate TLS, message có thể bị giải mã thành plaintext để routing, logging hoặc transform.

## 3. WS-Security bảo vệ gì?

WS-Security bảo vệ chính message.

Chữ ký, token, timestamp và mã hóa nằm bên trong SOAP message. Vì vậy, message có thể đi qua nhiều tầng trung gian mà vẫn giữ được bằng chứng:

- ai đã ký message
- message có bị sửa không
- phần nào được ký
- phần nào được mã hóa
- message có còn hiệu lực không
- certificate/token nào được dùng

So sánh ngắn gọn:

```text
HTTPS bảo vệ cái ống truyền dữ liệu.
WS-Security bảo vệ tài liệu nằm bên trong cái ống.
```

## 4. Chữ ký số trong WS-Security

Chữ ký số giúp đảm bảo:

1. Integrity: message không bị sửa.
2. Authentication: biết bên nào đã ký.
3. Non-repudiation: bên gửi khó phủ nhận nếu dùng private key/certificate hợp lệ.

Ví dụ client ký `SOAP Body`. Server nhận message và verify:

- signature có hợp lệ không
- certificate có được tin cậy không
- body có bị sửa sau khi ký không
- timestamp có còn hợp lệ không
- certificate có bị revoke không

Nếu ai đó sửa:

```xml
<Amount>1000000</Amount>
```

thành:

```xml
<Amount>9000000</Amount>
```

signature sẽ fail.

WS-Security chuẩn hóa cách ký từng phần XML message, ví dụ:

- ký toàn bộ body
- ký body và timestamp
- ký body và một số custom header
- reference đến element bằng `wsu:Id`
- gửi certificate kèm trong security header
- dùng canonicalization/digest/signature algorithm cụ thể

## 5. Mã hóa message

WS-Security có thể mã hóa một phần hoặc toàn bộ SOAP body.

Ví dụ chỉ mã hóa thông tin nhạy cảm:

```xml
<CardNumber>...</CardNumber>
```

hoặc mã hóa toàn bộ body:

```xml
<soap:Body>
  <xenc:EncryptedData>
    ...
  </xenc:EncryptedData>
</soap:Body>
```

Điều này hữu ích khi message đi qua trung gian:

```text
Client -> Gateway -> Broker -> Payment Service
```

Gateway cần đọc header để routing, nhưng không được đọc body payment. Khi đó:

- header routing để plain
- body nghiệp vụ được mã hóa cho Payment Service
- gateway vẫn chuyển tiếp được message nhưng không đọc được nội dung nhạy cảm

HTTPS không giải quyết tốt tình huống này nếu gateway terminate TLS, vì gateway sẽ thấy plaintext.

## 6. Token trong WS-Security

WS-Security hỗ trợ nhiều loại token.

### UsernameToken

```xml
<wsse:UsernameToken>
  <wsse:Username>partnerA</wsse:Username>
  <wsse:Password Type="PasswordDigest">...</wsse:Password>
  <wsse:Nonce>...</wsse:Nonce>
  <wsu:Created>2026-05-09T10:00:00Z</wsu:Created>
</wsse:UsernameToken>
```

Dùng cho xác thực username/password, thường kết hợp nonce và timestamp để giảm replay attack.

### BinarySecurityToken

Thường dùng để gửi X.509 certificate:

```xml
<wsse:BinarySecurityToken>
  <!-- base64 certificate -->
</wsse:BinarySecurityToken>
```

Server dùng certificate này để verify signature hoặc identify client.

### SAML Token

Trong enterprise, WS-Security có thể mang SAML assertion do Identity Provider phát hành. Service verify assertion để biết identity, role, permission hoặc federation context.

## 7. Timestamp, Nonce và Replay Attack

Replay attack là khi attacker lấy lại một request hợp lệ cũ và gửi lại.

Ví dụ:

```text
Transfer 1,000,000 VND từ A sang B
```

Nếu request cũ bị gửi lại, signature vẫn có thể hợp lệ vì message không bị sửa.

Vì vậy WS-Security thường dùng:

- `Timestamp`: message chỉ valid trong vài phút.
- `Nonce`: giá trị random chỉ dùng một lần.
- message ID: phát hiện duplicate.
- replay cache: server lưu nonce/message ID đã dùng.

## 8. REST vẫn tạo signature được không?

Có. REST hoàn toàn có thể tạo signature.

Ví dụ:

```http
POST /transfers
Content-Type: application/json
X-Client-Id: partnerA
X-Timestamp: 2026-05-09T10:00:00Z
X-Nonce: abc123
X-Signature: base64(hmac_sha256(...))
```

Body:

```json
{
  "fromAccount": "111",
  "toAccount": "222",
  "amount": 1000000
}
```

Server có thể verify signature dựa trên:

```text
HTTP method
path
query string
selected headers
timestamp
nonce
hash của body
```

Cách này rất phổ biến trong payment, fintech và public API.

## 9. Vậy tại sao cần SOAP/WS-Security?

Không cần SOAP chỉ để có signature.

Nếu yêu cầu chỉ là:

```text
Client -> API Server
HTTPS + OAuth2/JWT + request signature
```

thì REST thường gọn và tốt hơn.

SOAP/WS-Security có lý do tồn tại khi cần một bộ chuẩn enterprise cho message-level security:

- token format
- timestamp
- XML Signature
- XML Encryption
- signing parts
- encryption parts
- certificate reference
- SAML/X.509 integration
- end-to-end signature qua intermediary

REST cũng làm được, nhưng thường phải ghép thêm các chuẩn hoặc convention:

- HTTP Message Signatures
- JWS/JWE
- detached signature
- custom HMAC/RSA signing
- canonical JSON
- custom nonce/timestamp rule

Khác biệt cốt lõi:

```text
REST signature thường là convention của hệ thống hoặc tổ chức.
WS-Security là một framework chuẩn hóa riêng cho SOAP message security.
```

## 10. Điểm khó của REST signature

REST signature phải giải quyết canonicalization.

Ví dụ hai JSON sau có cùng ý nghĩa:

```json
{"amount":1000000,"toAccount":"222"}
```

```json
{
  "toAccount": "222",
  "amount": 1000000
}
```

Nếu signature tính trên raw body bytes, hai message trên cho ra signature khác nhau. Nếu tính trên canonical JSON, hai bên phải thống nhất canonicalization rule.

SOAP/XML Signature cũng phức tạp, nhưng canonicalization là một phần của chuẩn XML Signature/WS-Security nên enterprise tooling có sẵn quy tắc hơn.

## 11. Yếu tố lịch sử: vì sao WS-Security từng quan trọng?

WS-Security ra đời trong bối cảnh enterprise trước đây có nhiều tích hợp qua ESB, message broker, middleware và nhiều bên trung gian.

Kiến trúc phổ biến khi đó:

```text
Partner -> Gateway -> ESB -> Backend Service
```

Hoặc:

```text
Company A -> Clearing House -> Company B
```

Doanh nghiệp cần:

- ký message end-to-end
- mã hóa một phần message cho final receiver
- cho phép intermediary đọc header để routing
- gắn certificate/SAML token vào message
- audit message được lưu trong queue
- verify message sau khi nó đi qua nhiều tầng

HTTPS chỉ bảo vệ từng chặng transport. WS-Security giải quyết bài toán bảo vệ chính SOAP message.

Ngày nay, nhiều bài toán có thể dùng REST/gRPC kết hợp:

- HTTPS/mTLS
- OAuth2/OIDC/JWT
- API Gateway
- request signing
- JWS/JWE
- event signing
- OpenAPI/JSON Schema

Vì vậy REST có thể thay được SOAP trong phần lớn hệ thống mới. WS-Security còn phù hợp chủ yếu khi đối tác, vendor, compliance hoặc hệ thống legacy đã chuẩn hóa trên SOAP/WS-*.

## 12. Kết luận thực dụng

Không nên chọn SOAP chỉ vì cần signature.

Chọn REST nếu:

- xây API mới
- client là web/mobile/backend hiện đại
- cần developer experience tốt
- security có thể xử lý bằng HTTPS, OAuth2, JWT, mTLS, request signature
- không bị ràng buộc legacy

Chọn SOAP/WS-Security nếu:

- đối tác yêu cầu WSDL/SOAP
- enterprise stack đã dùng WS-Security
- cần interoperability với Java/.NET/ESB legacy
- cần message-level signing/encryption qua nhiều intermediary
- compliance/vendor chỉ chấp nhận SOAP/WS-*

Nói ngắn gọn:

```text
REST signature là đủ cho phần lớn hệ thống mới.
SOAP/WS-Security mạnh khi bạn đang ở trong hệ sinh thái enterprise legacy cần chuẩn WS-*.
```

