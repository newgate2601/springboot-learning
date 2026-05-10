# WS-Security

Tài liệu này tổng hợp các ý chính về WS-Security trong SOAP: nó là gì, khác HTTPS thế nào, UsernameToken PasswordDigest dùng khi nào, BinarySecurityToken/certificate là gì, Certificate Signature hoạt động ra sao, và cách chọn cơ chế phù hợp theo use case thực tế.

## 1. WS-Security là gì?

WS-Security là một chuẩn mở rộng cho SOAP, dùng để đưa thông tin bảo mật vào `SOAP Header`.

Nó cho phép SOAP message mang theo:

- token xác thực
- timestamp
- nonce
- chữ ký số
- dữ liệu mã hóa
- certificate
- reference đến phần message được ký hoặc mã hóa

Ví dụ SOAP message có WS-Security:

```xml
<soap:Envelope>
  <soap:Header>
    <wsse:Security>
      <wsu:Timestamp wsu:Id="TS-1">
        <wsu:Created>2026-05-10T03:00:00Z</wsu:Created>
        <wsu:Expires>2026-05-10T03:05:00Z</wsu:Expires>
      </wsu:Timestamp>

      <wsse:BinarySecurityToken
          wsu:Id="X509-1"
          ValueType="...#X509v3"
          EncodingType="...#Base64Binary">
        MIIC8DCCAdigAwIBAgI...
      </wsse:BinarySecurityToken>

      <ds:Signature>
        ...
      </ds:Signature>
    </wsse:Security>
  </soap:Header>

  <soap:Body wsu:Id="Body-1">
    <TransferMoneyRequest>
      <FromAccount>111</FromAccount>
      <ToAccount>222</ToAccount>
      <Amount>1000000</Amount>
    </TransferMoneyRequest>
  </soap:Body>
</soap:Envelope>
```

Trong thực tế, phần signature và encryption thường rất dài vì WS-Security dựa trên XML Signature và XML Encryption.

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

Nói ngắn gọn:

```text
HTTPS bảo vệ cái ống truyền dữ liệu.
WS-Security bảo vệ tài liệu nằm bên trong cái ống.
```

Thực tế có thể dùng cả hai:

```text
HTTPS + WS-Security
```

HTTPS bảo vệ kênh truyền. WS-Security bảo vệ chính SOAP message.

## 3. WS-Security bảo vệ gì?

WS-Security xử lý bốn nhóm vấn đề chính:

| Mục tiêu | Ý nghĩa |
|---|---|
| Authentication | Xác thực ai đang gọi service |
| Integrity | Đảm bảo message không bị sửa |
| Confidentiality | Mã hóa dữ liệu nhạy cảm |
| Replay protection | Ngăn request cũ bị gửi lại |

Vì chữ ký, token, timestamp và mã hóa nằm bên trong SOAP message, message có thể đi qua nhiều tầng trung gian mà vẫn giữ được bằng chứng:

- ai đã ký message
- message có bị sửa không
- phần nào được ký
- phần nào được mã hóa
- message có còn hiệu lực không
- certificate/token nào được dùng

## 4. UsernameToken là gì?

`UsernameToken` là cách đưa username/password hoặc password digest vào `SOAP Header`.

Ví dụ plain text:

```xml
<wsse:UsernameToken>
  <wsse:Username>partnerA</wsse:Username>
  <wsse:Password>secret</wsse:Password>
</wsse:UsernameToken>
```

Cách này chỉ nên dùng nếu bắt buộc và phải chạy qua HTTPS, vì password thật xuất hiện trong SOAP message.

Biến thể phổ biến hơn là `PasswordDigest`.

## 5. UsernameToken PasswordDigest

`UsernameToken PasswordDigest` không gửi password thật. Client gửi một digest được tính từ:

```text
PasswordDigest = Base64(SHA1(Nonce + Created + Password))
```

Trong đó:

- `Nonce`: chuỗi random dùng một lần
- `Created`: thời điểm tạo request
- `Password`: password thật hoặc shared secret

Ví dụ:

```xml
<wsse:UsernameToken>
  <wsse:Username>client-a</wsse:Username>

  <wsse:Password Type="...#PasswordDigest">
    bNNVbesNpUvKBgtMOUeYOQ3SA6c=
  </wsse:Password>

  <wsse:Nonce EncodingType="...#Base64Binary">
    WScqanjCEAC4mQoBE07sAQ==
  </wsse:Nonce>

  <wsu:Created>
    2026-05-10T03:00:00Z
  </wsu:Created>
</wsse:UsernameToken>
```

### Flow phía client

```text
1. Client có username/password đã thống nhất trước với server.
2. Client tạo Nonce random.
3. Client tạo Created = thời điểm hiện tại.
4. Client ghép Nonce + Created + Password.
5. Client tính SHA1.
6. Client encode kết quả bằng Base64.
7. Client gửi Username, PasswordDigest, Nonce, Created trong SOAP Header.
```

### Flow phía server

```text
1. Server nhận UsernameToken.
2. Server lấy username.
3. Server tìm password thật/shared secret tương ứng với username.
4. Server lấy Nonce và Created từ request.
5. Server tự tính lại Base64(SHA1(Nonce + Created + Password)).
6. Server so sánh digest tự tính với digest client gửi.
7. Server kiểm tra Created có quá cũ không.
8. Server kiểm tra Nonce đã từng được dùng chưa.
9. Nếu tất cả hợp lệ, request được chấp nhận.
```

Ví dụ:

```text
Client gửi:
Username = client-a
Nonce = abc123
Created = 2026-05-10T03:00:00Z
PasswordDigest = XYZ

Server biết:
Password thật của client-a = s3cr3t

Server tính lại:
Base64(SHA1("abc123" + "2026-05-10T03:00:00Z" + "s3cr3t"))

Nếu kết quả bằng XYZ, request hợp lệ.
```

### PasswordDigest chống được gì?

Nó giúp tránh gửi password thật trong SOAP message.

Nếu attacker bắt được request, họ chỉ thấy:

```text
username
digest
nonce
created
```

Không thấy password thật.

Tuy nhiên, nếu attacker gửi lại nguyên request cũ, có thể tạo replay attack. Vì vậy server phải kiểm tra:

- `Created` còn trong time window hợp lệ, ví dụ 5 phút
- `Nonce` chưa từng được dùng
- có thể kết hợp thêm message ID/replay cache

## 6. Certificate, public key và private key

Certificate trong WS-Security thường là X.509 certificate. Hiểu đơn giản, nó là chứng minh thư số của một hệ thống.

Certificate thường chứa:

- subject: tên chủ thể, ví dụ `client-app-A`
- public key: khóa công khai của client
- issuer: CA phát hành certificate
- valid from / valid to: thời gian hiệu lực
- serial number: mã định danh certificate
- chữ ký của CA để xác nhận certificate là thật

Điểm quan trọng:

```text
Certificate chứa public key.
Private key không nằm trong certificate public gửi qua SOAP.
```

Client giữ private key bí mật. Server dùng public key trong certificate để verify chữ ký.

```text
Private key: giữ bí mật, nằm ở client
Public key: chia sẻ cho server, thường nằm trong certificate
```

Khi client ký SOAP message:

```text
Client dùng private key để ký.
Server dùng public key trong certificate để verify.
```

Nếu verify thành công, server biết:

```text
Message được ký bởi bên giữ private key tương ứng.
Message không bị sửa sau khi ký.
```

## 7. BinarySecurityToken là gì?

`BinarySecurityToken` là một phần trong SOAP Header dùng để nhúng dữ liệu bảo mật dạng binary đã encode Base64. Trong use case X.509, nó thường chứa certificate.

Nói chính xác:

```text
BinarySecurityToken không chỉ chứa public key trần.
Nó thường chứa X.509 certificate.
Certificate đó chứa public key + thông tin định danh + issuer + hạn dùng + chữ ký của CA.
```

Ví dụ:

```xml
<wsse:BinarySecurityToken
    wsu:Id="X509-1"
    ValueType="...#X509v3"
    EncodingType="...#Base64Binary">
  MIIC8DCCAdigAwIBAgI...
</wsse:BinarySecurityToken>
```

Chuỗi `MIIC8DCC...` là certificate đã được encode Base64.

Nó không phải password. Nó cũng không phải private key.

Server đọc certificate này để lấy public key và verify chữ ký. Nhưng server vẫn phải kiểm tra certificate có đáng tin không, ví dụ:

- certificate có nằm trong truststore không
- certificate có được CA tin cậy phát hành không
- certificate còn hạn không
- certificate có bị revoke không, nếu hệ thống có kiểm tra CRL/OCSP

## 8. Certificate Signature

Certificate Signature là cách client ký SOAP message bằng private key. Server verify chữ ký bằng public key trong certificate.

Chữ ký số giúp đảm bảo:

| Mục tiêu | Ý nghĩa |
|---|---|
| Integrity | Message không bị sửa |
| Authentication | Biết bên nào đã ký |
| Non-repudiation | Bên gửi khó phủ nhận nếu private key/certificate được quản lý đúng |

Ví dụ client ký `SOAP Body` và `Timestamp`. Server nhận message rồi verify:

- signature có hợp lệ không
- certificate có được tin cậy không
- body có bị sửa sau khi ký không
- timestamp có còn hợp lệ không
- certificate có còn hạn hoặc có bị revoke không

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

- ký toàn bộ SOAP Body
- ký SOAP Body và Timestamp
- ký SOAP Body và một số custom header
- reference đến element bằng `wsu:Id`
- gửi certificate kèm trong security header
- dùng canonicalization, digest algorithm và signature algorithm cụ thể

## 9. Flow thực tế: ký SOAP bằng certificate

Giả sử ngân hàng B gọi SOAP API của ngân hàng A để chuyển tiền.

Client B có file:

```text
client-b.p12
```

File này thường chứa:

```text
Private key của Client B
Certificate của Client B
```

Server A có truststore chứa:

```text
Client B certificate
hoặc CA certificate đã phát hành certificate cho Client B
```

Flow:

```text
1. Client B tạo SOAP Body.
2. Client B thêm Timestamp.
3. Client B lấy private key từ file .p12.
4. Client B tạo digest/hash của SOAP Body + Timestamp.
5. Client B ký digest bằng private key.
6. Client B đưa chữ ký vào <ds:Signature>.
7. Client B nhúng certificate vào <wsse:BinarySecurityToken>, hoặc gửi reference để server tự tìm certificate.
8. Client B gửi SOAP request đến Server A.
9. Server A lấy certificate trong BinarySecurityToken hoặc tìm certificate theo reference.
10. Server A kiểm tra certificate có đáng tin không.
11. Server A lấy public key từ certificate.
12. Server A verify chữ ký.
13. Server A kiểm tra Timestamp.
14. Nếu hợp lệ, Server A xử lý nghiệp vụ.
```

SOAP Header rút gọn:

```xml
<soapenv:Header>
  <wsse:Security>

    <wsu:Timestamp wsu:Id="TS-1">
      <wsu:Created>2026-05-10T03:00:00Z</wsu:Created>
      <wsu:Expires>2026-05-10T03:05:00Z</wsu:Expires>
    </wsu:Timestamp>

    <wsse:BinarySecurityToken
        wsu:Id="X509-1"
        ValueType="...#X509v3"
        EncodingType="...#Base64Binary">
      MIIC8DCCAdigAwIBAgI...
    </wsse:BinarySecurityToken>

    <ds:Signature>
      <ds:SignedInfo>
        <ds:Reference URI="#Body-1"/>
        <ds:Reference URI="#TS-1"/>
      </ds:SignedInfo>

      <ds:SignatureValue>
        Vc3xk9...
      </ds:SignatureValue>

      <ds:KeyInfo>
        <wsse:SecurityTokenReference>
          <wsse:Reference URI="#X509-1"/>
        </wsse:SecurityTokenReference>
      </ds:KeyInfo>
    </ds:Signature>

  </wsse:Security>
</soapenv:Header>
```

Ý nghĩa:

```text
BinarySecurityToken chứa certificate.
Signature chứa chữ ký.
KeyInfo chỉ cho server biết dùng certificate nào để verify.
```

## 10. Có bắt buộc nhúng certificate vào message không?

Không phải lúc nào cũng cần.

### Kiểu 1: Nhúng certificate vào BinarySecurityToken

```text
Client gửi certificate trong SOAP Header.
Server lấy public key từ đó để verify.
```

Ưu điểm:

- server dễ biết dùng certificate nào
- phù hợp khi client có nhiều certificate hoặc nhiều partner

Nhược điểm:

- message dài hơn
- server vẫn phải kiểm tra certificate có được tin cậy không

### Kiểu 2: Không nhúng certificate

Client chỉ gửi thông tin tham chiếu, ví dụ issuer/serial number hoặc key identifier.

```text
Server tự tìm certificate trong truststore.
```

Ưu điểm:

- message gọn hơn
- phù hợp khi hai bên đã trao đổi certificate trước

Nhược điểm:

- server phải mapping đúng certificate
- vận hành truststore phải rõ ràng

## 11. Mã hóa message

WS-Security có thể mã hóa một phần hoặc toàn bộ SOAP Body.

Ví dụ mã hóa thông tin nhạy cảm:

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

Một flow vừa ký vừa mã hóa có thể như sau:

```text
Client dùng public key của server để mã hóa dữ liệu nhạy cảm.
Server dùng private key của server để giải mã.
Client dùng private key của client để ký message.
Server dùng certificate của client để verify chữ ký.
```

Ở đây có thể có hai certificate:

```text
Client certificate: để server verify chữ ký client.
Server certificate: để client mã hóa dữ liệu gửi cho server.
```

## 12. Timestamp, Nonce và Replay Attack

Replay attack là khi attacker lấy lại một request hợp lệ cũ và gửi lại.

Ví dụ:

```text
Transfer 1,000,000 VND từ A sang B
```

Nếu request cũ bị gửi lại, signature vẫn có thể hợp lệ vì message không bị sửa.

Vì vậy WS-Security thường dùng:

- `Timestamp`: message chỉ valid trong vài phút
- `Nonce`: giá trị random chỉ dùng một lần
- message ID: phát hiện duplicate
- replay cache: server lưu nonce/message ID đã dùng

Ví dụ Timestamp:

```xml
<wsu:Timestamp>
  <wsu:Created>2026-05-10T03:00:00Z</wsu:Created>
  <wsu:Expires>2026-05-10T03:05:00Z</wsu:Expires>
</wsu:Timestamp>
```

Với Certificate Signature, nên ký cả `Timestamp` để tránh timestamp bị sửa.

Với UsernameToken PasswordDigest, server phải kiểm tra `Nonce` và `Created`. Nếu không kiểm tra nonce, digest vẫn có thể bị replay trong thời gian ngắn.

## 13. UsernameToken PasswordDigest dùng khi nào?

UsernameToken PasswordDigest phù hợp khi hai bên có thể chia sẻ password/secret trước, và mục tiêu chính là xác thực client.

Nó phù hợp với:

- internal SOAP service
- partner integration đơn giản
- hệ thống legacy chỉ hỗ trợ username/password
- service chạy sau HTTPS, không cần ký từng SOAP Body
- API rủi ro thấp hoặc vừa phải
- client là ứng dụng server-side, không phải mobile/browser public

Ví dụ:

```text
CRM gọi SOAP service của Billing nội bộ.
```

Flow:

```text
CRM và Billing thống nhất:
username = crm-app
password = shared-secret

CRM gửi SOAP request có UsernameToken PasswordDigest.
Billing kiểm tra digest, nonce, created.
Nếu hợp lệ thì xử lý request.
```

Ở case này, hệ thống chỉ cần biết:

```text
Request đến từ crm-app hợp lệ.
Request không dùng lại nonce cũ.
Request không quá hạn thời gian.
```

Không nhất thiết phải ký toàn bộ SOAP Body.

Ví dụ API phù hợp:

```text
getProductCatalog()
getExchangeRate()
getBranchList()
getProvinceList()
checkOrderStatus()
getCustomerRewardPoints()
```

Các API này thường:

- read-only
- rủi ro thấp hơn giao dịch tài chính
- chạy qua HTTPS
- nằm trong internal network hoặc VPN
- không có nhiều intermediary không tin cậy
- không cần chứng minh pháp lý nội dung request

## 14. Certificate Signature dùng khi nào?

Certificate Signature phù hợp khi cần xác thực mạnh và đảm bảo message không bị sửa.

Nó phù hợp với:

- B2B integration quan trọng
- banking, payment, insurance, government
- request thay đổi tiền, quyền lợi, hồ sơ pháp lý
- message đi qua gateway, ESB, middleware
- cần ký SOAP Body để chống sửa nội dung
- hai bên không muốn chia sẻ password chung
- cần quản lý bằng certificate, expiry, rotation, CA
- cần audit mạnh xem bên nào đã gửi nội dung gì

Ví dụ:

```text
Ví điện tử gọi SOAP API của ngân hàng để tạo giao dịch rút tiền.
```

Request:

```xml
<transfer>
  <fromWallet>W001</fromWallet>
  <toBankAccount>123456789</toBankAccount>
  <amount>10000000</amount>
</transfer>
```

Dùng:

```text
HTTPS + X.509 Certificate Signature + Timestamp
```

Flow:

```text
1. Ví điện tử tạo SOAP Body transfer.
2. Ký SOAP Body bằng private key.
3. Gửi certificate trong BinarySecurityToken hoặc tham chiếu certificate.
4. Ngân hàng verify certificate có tin cậy không.
5. Ngân hàng dùng public key verify chữ ký.
6. Nếu amount, account hoặc timestamp bị sửa, chữ ký fail.
7. Ngân hàng xử lý giao dịch.
```

Ở đây Certificate Signature giải quyết vấn đề quan trọng:

```text
Không ai có thể sửa amount từ 10,000,000 thành 90,000,000 mà vẫn giữ chữ ký hợp lệ.
```

## 15. UsernameToken PasswordDigest có thay được Certificate Signature không?

Không thay thế tương đương.

Hai cơ chế này không cùng hạng:

```text
UsernameToken PasswordDigest = chứng minh client biết shared secret.
Certificate Signature = chứng minh client giữ private key + bảo vệ nội dung message không bị sửa.
```

UsernameToken PasswordDigest chủ yếu trả lời:

```text
Ai đang gọi service?
Client này có biết password/secret hợp lệ không?
```

Certificate Signature trả lời thêm:

```text
Ai đã ký message này?
SOAP Body có bị sửa không?
Timestamp có bị sửa không?
Nội dung request có thể kiểm chứng end-to-end không?
```

Nếu use case cần integrity của message, audit mạnh, chống sửa body, hoặc giao dịch quan trọng thì UsernameToken PasswordDigest không thay được Certificate Signature.

Tuy nhiên UsernameToken PasswordDigest có lợi thế riêng:

- dễ implement hơn
- không cần private key, public key, keystore, truststore
- không cần certificate lifecycle
- không cần CA, expiry, rotation phức tạp
- dễ debug bằng log SOAP
- phù hợp legacy SOAP stack
- phù hợp hệ thống nội bộ đã có HTTPS và network control

Certificate Signature mạnh hơn nhưng có chi phí vận hành:

- cấu hình phức tạp hơn
- phải quản lý keystore/truststore
- phải bảo vệ private key nghiêm ngặt
- certificate hết hạn sẽ làm tích hợp lỗi
- rotation certificate cần phối hợp hai bên
- XML Signature dễ lỗi canonicalization, namespace, reference ID
- debug khó hơn
- tốn CPU hơn UsernameToken
- có thể khó tích hợp với hệ thống legacy cũ

Các lỗi thực tế hay gặp:

- verify signature fail vì khác XML canonicalization
- certificate expired
- partner đổi certificate nhưng chưa báo
- truststore thiếu intermediate CA
- clock skew làm timestamp fail
- ký sai phần body
- Reference URI không match `wsu:Id`

## 16. So sánh UsernameToken PasswordDigest và Certificate Signature

| Tiêu chí | UsernameToken PasswordDigest | Certificate Signature |
|---|---|---|
| Mục tiêu chính | Xác thực client | Xác thực + chống sửa message |
| Client giữ gì | Password/shared secret | Private key |
| Server giữ gì | Password hoặc secret | Certificate/CA trust |
| Có bảo vệ SOAP Body khỏi bị sửa không | Không đủ mạnh nếu đứng một mình | Có, nếu ký đúng phần cần bảo vệ |
| Có chống replay không | Có nếu kiểm tra Nonce + Created | Có nếu ký Timestamp + kiểm tra expiry/message ID |
| Độ phức tạp | Thấp | Cao |
| Vận hành | Dễ | Khó hơn |
| Debug | Dễ hơn | Khó hơn |
| Phù hợp read-only/internal | Tốt | Có thể quá nặng |
| Phù hợp payment/legal/high-risk | Không nên đứng một mình | Tốt |

Quy tắc thực dụng:

| Use case | Nên dùng |
|---|---|
| Service nội bộ, sau HTTPS, rủi ro thấp | UsernameToken PasswordDigest |
| Partner đơn giản, query dữ liệu ít nhạy cảm | UsernameToken PasswordDigest + HTTPS |
| Giao dịch tiền, hồ sơ pháp lý, dữ liệu nhạy cảm | Certificate Signature |
| Message qua gateway/ESB và cần end-to-end integrity | Certificate Signature |
| Cần mã hóa field nhạy cảm trong SOAP | Certificate Encryption |
| Cần audit mạnh xem bên nào đã gửi nội dung gì | Certificate Signature |
| Legacy system chỉ hỗ trợ user/pass | UsernameToken |
| Không muốn quản lý keystore/certificate | UsernameToken |
| Không muốn lưu shared password phía server | Certificate Signature |

Một cách hỏi đúng hơn:

```text
Use case này chỉ cần authentication,
hay cần authentication + message integrity?
```

Nếu chỉ cần authentication:

```text
UsernameToken PasswordDigest thường đủ.
```

Nếu cần message integrity:

```text
Certificate Signature.
```

Nếu cần confidentiality ở message level:

```text
Certificate Encryption.
```

## 17. Ví dụ chọn cơ chế theo API

### Case 1: Read-only nội bộ

```text
getDepartments()
getBranchList()
getExchangeRate()
```

Yêu cầu:

- chỉ app nội bộ được gọi
- không có giao dịch tiền
- đã chạy HTTPS trong private network
- không cần ký body

Lựa chọn hợp lý:

```text
HTTPS + UsernameToken PasswordDigest
```

### Case 2: Query partner rủi ro vừa phải

```text
getOrderStatus(orderId)
getCustomerRewardPoints(customerId)
```

Yêu cầu:

- xác thực partner
- tránh gửi password thật
- chống replay cơ bản bằng nonce/timestamp

Lựa chọn hợp lý:

```text
HTTPS + UsernameToken PasswordDigest
```

Nếu dữ liệu nhạy cảm hơn, có thể cân nhắc thêm signature hoặc chuyển sang cơ chế certificate.

### Case 3: Thay đổi giá trị tài sản

```text
redeemRewardPoints(customerId, amount)
transferMoney(fromAccount, toAccount, amount)
approveLoan(loanId, amount)
```

Yêu cầu:

- xác thực partner mạnh
- không được sửa amount/account/customerId
- cần audit nội dung đã gửi
- chống replay

Lựa chọn hợp lý:

```text
HTTPS + Certificate Signature + Timestamp
```

Có thể thêm:

```text
Encryption cho account number hoặc PII.
```

### Case 4: Message đi qua nhiều intermediary

```text
Client -> API Gateway -> ESB -> Core SOAP Service
```

Nếu chỉ HTTPS:

```text
Client -> Gateway: TLS
Gateway -> ESB: TLS
ESB -> Core: TLS
```

Mỗi tầng có thể đọc hoặc thay đổi message sau khi terminate TLS.

Nếu SOAP Body được ký:

```text
Client ký body.
Gateway/ESB có thể route message.
Core service verify chữ ký cuối cùng.
```

Nếu Gateway hoặc ESB sửa nội dung nghiệp vụ, Core sẽ phát hiện.

## 18. REST vẫn tạo signature được không?

Có. REST hoàn toàn có thể tạo request signature.

Ví dụ:

```http
POST /transfers
Content-Type: application/json
X-Client-Id: partnerA
X-Timestamp: 2026-05-10T03:00:00Z
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

## 19. Vậy tại sao cần SOAP/WS-Security?

Không cần SOAP chỉ để có signature.

Nếu yêu cầu chỉ là:

```text
Client -> API Server
HTTPS + OAuth2/JWT + request signature
```

thì REST thường gọn và tốt hơn cho hệ thống mới.

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

## 20. Điểm khó của REST signature

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

## 21. Yếu tố lịch sử: vì sao WS-Security từng quan trọng?

WS-Security ra đời trong bối cảnh enterprise trước đây có nhiều tích hợp qua ESB, message broker, middleware và nhiều bên trung gian.

Kiến trúc phổ biến:

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

## 22. Điểm cần cẩn thận khi dùng WS-Security

WS-Security mạnh nhưng dễ cấu hình sai.

Cần chú ý:

- không dùng password plain text nếu không có HTTPS
- timestamp nên có thời gian hết hạn ngắn
- phải kiểm tra nonce/message ID để chống replay
- private key phải được bảo vệ kỹ
- certificate cần có lifecycle rõ ràng: expiry, rotation, revocation
- phải ký đúng phần quan trọng, thường là SOAP Body và Timestamp
- cần tránh XML Signature Wrapping, tức attacker chèn body giả để đánh lừa service
- phải thống nhất thuật toán ký, digest, canonicalization giữa hai bên
- cần đồng bộ thời gian hệ thống để tránh lỗi timestamp do clock skew

## 23. Kết luận thực dụng

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

Kết luận ngắn:

```text
UsernameToken PasswordDigest:
Dùng để xác thực client bằng shared password.
Phù hợp hệ thống đơn giản, nội bộ, read-only, hoặc rủi ro vừa phải.

Certificate Signature:
Dùng để xác thực bằng private key/certificate và bảo vệ integrity của SOAP message.
Phù hợp giao dịch quan trọng, B2B enterprise, banking, payment, government,
hoặc message đi qua nhiều tầng trung gian.

BinarySecurityToken:
Thường là nơi nhúng X.509 certificate trong SOAP Header.
Certificate chứa public key và thông tin định danh, không chứa private key public gửi qua message.
```

Nếu request có tác động tài chính/pháp lý hoặc cần chứng minh nội dung không bị sửa, chọn Certificate Signature. Nếu chỉ cần biết client nào đang gọi API trong môi trường đã có HTTPS/VPN, UsernameToken PasswordDigest thường đủ và dễ vận hành hơn.
